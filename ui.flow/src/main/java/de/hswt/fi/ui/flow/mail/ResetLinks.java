package de.hswt.fi.ui.flow.mail;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * The link of the password reset mail. It is built from the configured public address of the site
 * ({@code de.hswt.fi.public-url}, environment variable {@code FI_PUBLIC_URL}), not from the Host header of the request,
 * so a forged header cannot send the token to another host (SP-20, review finding S7).
 */
public final class ResetLinks {

	public static final String PATH = "/reset-password";

	private ResetLinks() {
	}

	/**
	 * The reset link below {@code publicUrl}, or {@code null} if no public address is configured.
	 *
	 * @throws IllegalArgumentException if {@code publicUrl} is not an absolute http(s) address
	 */
	public static String fromPublicUrl(String publicUrl, String token) {
		if (publicUrl == null || publicUrl.isBlank()) {
			return null;
		}
		URI base = URI.create(publicUrl.trim());
		if (base.getHost() == null || !("https".equals(base.getScheme()) || "http".equals(base.getScheme()))
				|| base.getQuery() != null || base.getFragment() != null) {
			throw new IllegalArgumentException("de.hswt.fi.public-url must be an absolute http(s) address without query");
		}
		String basePath = base.getPath() == null ? "" : base.getPath().replaceAll("/+$", "");
		return UriComponentsBuilder.newInstance().scheme(base.getScheme()).host(base.getHost()).port(base.getPort())
				.path(basePath + PATH).queryParam("token", token).build().encode().toUriString();
	}
}
