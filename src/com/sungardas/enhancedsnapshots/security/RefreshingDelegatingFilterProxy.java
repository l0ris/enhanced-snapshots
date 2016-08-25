package com.sungardas.enhancedsnapshots.security;


import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.GenericApplicationListenerAdapter;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import java.lang.reflect.Field;


public class RefreshingDelegatingFilterProxy extends DelegatingFilterProxy implements ApplicationListener<ContextRefreshedEvent> {

    private boolean listenerRegistered = false;


    @Override
    protected Filter initDelegate(WebApplicationContext wac) throws ServletException {
        Filter filter = super.initDelegate(wac);
        if(!listenerRegistered) {
            ((AbstractApplicationContext) wac).getApplicationListeners().add(new GenericApplicationListenerAdapter(this));
            listenerRegistered = true;
        }
        return filter;
    }

    // TODO: change this in case next bug in spring security project will be resolved:
    // TODO: https://jira.spring.io/browse/SPR-6228
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            Field field = DelegatingFilterProxy.class.getDeclaredField("delegate");
            field.setAccessible(true);
            field.set(this, null);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}