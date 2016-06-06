package com.sungardas.enhancedsnapshots.rest.filters;


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


public class FilterProxy implements Filter {

	private Filter filter;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (filter == null) {
			chain.doFilter(request, response);
		} else {
			filter.doFilter(request, response, chain);
		}
	}

	@Override
	public void destroy() {

	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}
}
