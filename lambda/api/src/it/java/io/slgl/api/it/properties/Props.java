package io.slgl.api.it.properties;

import lombok.SneakyThrows;

import java.util.List;

public class Props {

	private static final PropertyLoader properties = loadProperties();

	@SneakyThrows
	private static PropertyLoader loadProperties() {
		return new CompoundPropertyLoader(List.of(
				new SystemPropertyLoader(),
				new PropertiesFilePropertyLoader("/etc/slgl/test.properties", "io.slgl.it.properties"),
				new AwsStackPropertyLoader()
		));
	}


	public static SlglProperties getSlglProperties() {
		return new SlglProperties(properties);
	}
}
