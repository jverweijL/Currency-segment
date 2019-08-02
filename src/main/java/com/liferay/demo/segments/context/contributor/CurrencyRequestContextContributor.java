/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.demo.segments.context.contributor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.segments.context.Context;
import com.liferay.segments.context.contributor.RequestContextContributor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;

/**
 * @author Jan
 */
@Component(
	immediate = true,
	property = {
		"request.context.contributor.key=" + CurrencyRequestContextContributor.KEY,
		"request.context.contributor.type=String"
	},
	service = RequestContextContributor.class
)
public class CurrencyRequestContextContributor
	implements RequestContextContributor {

	public static final String KEY = "currency";
	String myip = PortalUtil.getPortalProperties().getProperty("segment.currency.myip",null);
	String apiKey = PortalUtil.getPortalProperties().getProperty("segment.ipgeolocation.apikey",null);

	@Override
	public void contribute(
		Context context, HttpServletRequest httpServletRequest) {

		HttpSession session = httpServletRequest.getSession();

		if (context.get(KEY) == null) {

			try {
				String ipAddress = getIpAddress(httpServletRequest);
				_log.debug(String.format("Using ip %s", ipAddress));

				if (!ipAddress.startsWith("127.") && !ipAddress.startsWith("192.") && !ipAddress.startsWith("10.")) {
					HttpUriRequest request = RequestBuilder
							//.get("https://ipapi.co/" + ipAddress + "/json")
							.get(String.format("https://api.ipgeolocation.io/ipgeo?apiKey=%s&ip=%s",apiKey,ipAddress))
							.build();

					ObjectMapper mapper = new ObjectMapper();
					JsonNode jsonResult = mapper.readTree(getRequest(request));

					String currency = jsonResult.get("currency").get("code").asText("USD");

					_log.debug(String.format("Using currency %s", currency));
					session.setAttribute(KEY, currency);
					context.put(KEY, currency);
				}

			} catch (Exception e) {
				_log.debug(e.getMessage());
				_log.debug("Global exception: " + e.toString());
			}
		}
	}

	private String getRequest(HttpUriRequest request) {
		HttpClient client = HttpClientBuilder.create().build();

		HttpResponse response = null;
		try {
			response = client.execute(request);
			HttpEntity entity = response.getEntity();
			if (response.getStatusLine().getStatusCode() == 200) {
				return EntityUtils.toString(entity, "UTF-8");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}

	private String getIpAddress (HttpServletRequest request) {

		if (myip != null) {
			return myip;
		}

		String ipAddress = request.getHeader("X-Real-Ip");
		if (ipAddress == null || ipAddress.isEmpty()) {
			ipAddress = request.getHeader("X-Forwarded-For");
		}
		if (ipAddress == null || ipAddress.isEmpty()) {
			ipAddress = request.getRemoteAddr();
		}

		return ipAddress.replaceFirst(",.*","").trim();
	}

	private static final Log _log = LogFactoryUtil.getLog(CurrencyRequestContextContributor.class);
}