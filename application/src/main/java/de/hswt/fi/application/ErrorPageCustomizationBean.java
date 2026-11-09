package de.hswt.fi.application;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This class is used to customize the error pages of the embedded servlet
 * container. Static pages must be placed under the folder /static e.g. in the
 * module de.hswt.fi.application
 *
 * @author August Gilg
 */
@Controller
public class ErrorPageCustomizationBean implements ErrorController {

	private static final String ERROR_PATH = "/error";

	@RequestMapping(ERROR_PATH)
	public String customize(HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

		if (status != null) {
			int statusCode = Integer.parseInt(status.toString());

			if (statusCode == HttpStatus.NOT_FOUND.value()) {
				return "404";
			}
		}
		return "error";
	}

	// getErrorPath() was removed in Spring Boot 2.5; the error path is server.error.path (default /error)
}
