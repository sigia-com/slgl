package io.slgl.client.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.slgl.client.error.*;
import io.slgl.client.utils.jackson.ObjectMapperFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public final class RestClient implements AutoCloseable {

	private final String baseUrl;
	private final String username;
	private final String apiKey;
	private final HttpRequestExecutor http;

	private final ObjectMapper objectMapper;
	private final UrlToolbox uriToolbox;

	public RestClient(String baseUrl, String username, String apiKey, HttpRequestExecutor http) {
		this.baseUrl = baseUrl;
		this.username = username;
		this.apiKey = apiKey;
		this.http = requireNonNull(http);

		this.objectMapper = ObjectMapperFactory.createSlglObjectMapper();
		this.uriToolbox = new UrlToolbox(objectMapper);
	}

	public <T> T get(String path, Object queryParams, Class<T> responseType) {
		HttpGet httpGet = new HttpGet(uriToolbox.buildUrl(baseUrl, path, queryParams));
		return doRequest(httpGet, type(responseType));
	}

	public <T> T post(String path, Object request, Class<T> responseType) {
		return post(path, request, type(responseType));
	}

	public <T> List<T> postForList(String path, Object request, Class<T> elementClass) {
		return post(path, request, listType(elementClass));
	}

	private <T> T post(String path, Object request, JavaType responseType) {
		HttpPost httpPost = new HttpPost(uriToolbox.buildUrl(baseUrl, path));
		httpPost.setEntity(createRequestEntity(request));
		return doRequest(httpPost, responseType);
	}

	private StringEntity createRequestEntity(Object request) {
		if (request instanceof String) {
			return new StringEntity(((String) request), ContentType.APPLICATION_JSON);
		}

		try {
			String result = objectMapper.writer().writeValueAsString(request);
			return new StringEntity(result, ContentType.APPLICATION_JSON);
		} catch (JsonProcessingException e) {
			throw new SlglNetworkException(e);
		}
	}

	private <T> T doRequest(HttpUriRequest request, JavaType responseType) {
		addAuthorizationHeader(request);

		try {
			HttpResponse response = http.execute(request);
			validateResponseContent(response);
			handleErrorResponse(response);

			return parseResponse(responseType, response);

		} catch (IOException e) {
			throw new SlglNetworkException(e);
		}
	}

	private void validateResponseContent(HttpResponse response) throws IOException {
		HttpEntity httpEntity = response.getEntity();
		if (httpEntity == null) {
			throw new SlglResponseMappingException("No HTTP response body", response.getStatusLine().getStatusCode());
		}

		ContentType contentType = ContentType.get(httpEntity);
		if (contentType != null && ContentType.APPLICATION_JSON.getMimeType().equals(contentType.getMimeType())) {
			return;
		}
		try (InputStream responseContent = httpEntity.getContent()) {
			String responseText = readResponseContent(responseContent);
			throw new SlglInvalidContentTypeException(contentType, responseText, response.getStatusLine().getStatusCode());
		}
	}

	private void addAuthorizationHeader(HttpRequest request) {
		if (username == null && apiKey == null) {
			return;
		}

		String credentials = urlEncode(username) + ":" + apiKey;
		String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(UTF_8));
		request.setHeader("Authorization", "Basic " + encodedCredentials);
	}

	private String urlEncode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SlglClientException(e);
		}
	}

	private void handleErrorResponse(HttpResponse response) throws IOException {
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 200) {
			return;
		}

		ErrorResponseWrapper errorResponseWrapper = parseErrorResponse(response);
		if (errorResponseWrapper.getError() == null) {
			throw new SlglResponseMappingException("Empty error response", response.getStatusLine().getStatusCode());
		}

		throw new SlglApiException(errorResponseWrapper.getError(), statusCode);
	}

	private <T> T parseResponse(JavaType responseType, HttpResponse response) throws IOException {
		try (InputStream responseContent = response.getEntity().getContent()) {
			return objectMapper.reader().forType(responseType).readValue(responseContent);
		} catch (JsonProcessingException e) {
			throw new SlglResponseMappingException(e, response.getStatusLine().getStatusCode());
		}
	}

	private ErrorResponseWrapper parseErrorResponse(HttpResponse response) throws IOException {
		try (InputStream responseContent = response.getEntity().getContent()) {
			String responseText = readResponseContent(responseContent);
			try {
				return objectMapper.reader().forType(ErrorResponseWrapper.class).readValue(responseText);
			} catch (JsonProcessingException e) {
				throw new SlglResponseMappingException("Unrecognized error response content: " + responseText, response.getStatusLine().getStatusCode(), e);
			}
		}
	}

	private String readResponseContent(InputStream responseContent) {
		return new Scanner(responseContent).useDelimiter("\\A").next();
	}

	private JavaType type(Class<?> responseType) {
		return objectMapper.constructType(responseType);
	}

	private CollectionType listType(Class<?> elementType) {
		return objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
	}

	@Override
	public void close() throws Exception {
		http.close();
	}
}
