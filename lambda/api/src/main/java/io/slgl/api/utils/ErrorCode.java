package io.slgl.api.utils;

import lombok.Getter;

import static org.apache.http.HttpStatus.*;

@Getter
public enum ErrorCode {

	NODE_NOT_FOUND(SC_NOT_FOUND, "node_not_found", "Node not found"),
	EMPTY_QUERY_NOT_SUPPORTED(SC_BAD_REQUEST, "empty_query_not_supported", "Empty query is not supported"),

	VALIDATION_ERROR(SC_BAD_REQUEST, "validation_error", "Request contains validation errors"),
	VALIDATION_ERROR_WITH_MESSAGE(SC_BAD_REQUEST, "validation_error", "Request contains validation errors:\n%s"),

	RESERVED_FIELD(SC_BAD_REQUEST, "reserved_field", "The request contains a reserved field: %s."),

	NODE_ALREADY_EXISTS(SC_CONFLICT, "node_already_exists", "Node with given @id already exists."),
	LINK_ALREADY_EXISTS(SC_CONFLICT, "link_already_exists", "Link with given data already exists."),

	LINK_NOT_FOUND(SC_BAD_REQUEST, "link_not_found", "Link with given id doesn't exist: %s"),

	LINKING_SOURCE_NODE_NOT_FOUND(SC_BAD_REQUEST, "linking_source_node_not_found", "Link source node doesn't exist: %s"),
	LINKING_TARGET_NODE_NOT_FOUND(SC_BAD_REQUEST, "linking_target_node_not_found", "Link target node doesn't exist: %s"),
	LINKING_TARGET_ANCHOR_NOT_FOUND(SC_BAD_REQUEST, "linking_target_anchor_not_found", "Link target anchor doesn't exist: %s"),
	LINKING_TO_TYPE(SC_BAD_REQUEST, "linking_to_type", "Linking to type node is not allowed: %s"),
    LINKING_NODE_TYPE_NOT_MATCHING_ANCHOR_TYPE(SC_BAD_REQUEST, "linking_node_type_not_matching_anchor_type", "Type of node being linked doesn't match type of anchor"),
	LINKING_TO_ALREADY_INLINED_ANCHOR(SC_BAD_REQUEST, "linking_to_inline_anchor", "Linking to anchor that is already defined inline is not allowed."),
    LINKED_DOCUMENT_MUST_BE_PDF(SC_BAD_REQUEST, "linked_document_must_be_pdf", "Only pdf documents linking is currently supported"),
	LINKING_TO_NODE_WITH_OBSERVERS_USING_NODE_NOT_PROVIDED_IN_THIS_REQUEST(SC_BAD_REQUEST, "linking_to_node_with_observers_using_node_not_provided_in_this_request", "Linking to node with observers can only be done using node created in this request or by providing request for existing node."),

	PERMISSION_DENIED(SC_FORBIDDEN, "permission_denied", "You do not have permissions to do requested action."),

	TYPE_DOESNT_EXIST(SC_BAD_REQUEST, "type_doesnt_exist", "Type doesn't exist."),
	TYPE_IS_NOT_TYPE(SC_BAD_REQUEST, "type_is_not_type", "Requested @type is not a type node."),
	CAMOUFLAGED_TYPE_IS_NOT_READABLE(SC_BAD_REQUEST, "camouflaged_type_is_not_readable", "Camouflaged type can't be read"),

	AUTHORIZATION_DOESNT_EXIST(SC_BAD_REQUEST, "authorization_doesnt_exist", "Authorization doesn't exist."),
	AUTHORIZATION_HAS_INVALID_TYPE(SC_BAD_REQUEST, "authorization_has_invalid_type", "Authorization has invalid type."),
	AUTHORIZATION_PERMISSION_DENIED(SC_FORBIDDEN, "authorization_denied", "You do not have permissions to use requested Authorization."),
	AUTHORIZATION_NOT_MATCHING_ACTION(SC_FORBIDDEN, "authorization_denied", "Authorization doesn't match action that you are trying to execute."),

	DOMAIN_NOT_VERIFIED(SC_FORBIDDEN, "domain_not_verified", "You are not verified to use domain given in @id attribute: %s"),
	DOMAIN_CANNOT_BE_BLANK(SC_BAD_REQUEST, "domain_cannot_be_blank", "Adding domain must have a request with non-blank `value` field"),

	UNABLE_TO_PARSE_TEMPLATE(SC_BAD_REQUEST, "unable_to_parse_template", "Unable to parse template: %s"),

	OBSERVER_UNIQUE_RECOVERY_STORAGE_PATHS(SC_BAD_REQUEST, "observer_unique_recovery_storage_paths", "Observer's recovery storage paths must be unique: %s"),
	OBSERVER_UNIQUE_STORAGE_LOCATIONS(SC_BAD_REQUEST, "observer_unique_storage_locations", "Observer's storage locations must be unique: %s"),

	TOO_MANY_REQUESTS_IN_BATCH(SC_BAD_REQUEST, "too_many_requests_in_batch", "Too many requests in batch. Total weight of requests cannot be bigger than 20 (node:1; link:1; unlink:2)."),

	SIGNATURE_MALFORMED(SC_BAD_REQUEST, "invalid_request", "The document was signed, but the signature is not valid."),
	SIGNATURE_MISSING(SC_BAD_REQUEST, "invalid_request", "Linked document seems to not be signed."),
	MISSING_SUBFILTER(SC_BAD_REQUEST, "invalid_request", "One of the signatures has no subFilter."),
	SUB_FILTER_UNSUPPORTED(SC_BAD_REQUEST, "invalid_request", "PDF certificate's subFilter: %s is not supported"),

	UNABLE_TO_PARSE_REQUEST(SC_BAD_REQUEST, "unable_to_parse_request", "Unable to parse request"),
	UNABLE_TO_PARSE_FIELD(SC_BAD_REQUEST, "unable_to_parse_request", "Unable to parse request - unable to parse field: %s"),
	UNABLE_TO_PARSE_UNRECOGNIZED_FIELD(SC_BAD_REQUEST, "unable_to_parse_request", "Unable to parse request - unrecognized field: %s"),

	INVALID_API_KEY(SC_UNAUTHORIZED, "invalid_api_key", "Provided API key is invalid."),
	NO_API_CREDITS_LEFT(SC_FORBIDDEN, "no_api_credits_left", "User does not have any API credits left."),

	STATE_DELETED(SC_BAD_REQUEST, "state_deleted", "State was deleted."),
	STATE_HASH_MISMATCH(SC_BAD_REQUEST, "state_hash_mismatch", "Provided state doesn't match state hash of node: %s"),
	STATE_REQUIRED(SC_BAD_REQUEST, "state_required", "State is required but not provided for node: %s"),
	OBJECT_HASH_MISMATCH(SC_BAD_REQUEST, "object_hash_mismatch", "Provided request doesn't match object hash of node: %s"),
	FILE_HASH_MISMATCH(SC_BAD_REQUEST, "file_hash_mismatch", "Provided request doesn't match file hash of node: %s"),

	CONCURRENCY_CONFLICT(SC_CONFLICT, "concurrency_conflict", "Conflict between multiple concurrent requests"),

	HTTP_PATH_NOT_FOUND(SC_NOT_FOUND, "path_not_found", "Requested URL was not found"),
	HTTP_METHOD_NOT_SUPPORTED(SC_METHOD_NOT_ALLOWED, "http_method_not_supported", "Requested HTTP method is not supported for requested URL"),
	UNKNOWN_ERROR(SC_INTERNAL_SERVER_ERROR, "unknown_error", "Unknown error when handling your request");

	private final int httpStatus;
	private final String code;
	private final String message;

	ErrorCode(int httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public String getMessage(Object... params) {
		if (params == null || params.length == 0) {
			return getMessage();
		}

		return String.format(getMessage(), params);
	}
}
