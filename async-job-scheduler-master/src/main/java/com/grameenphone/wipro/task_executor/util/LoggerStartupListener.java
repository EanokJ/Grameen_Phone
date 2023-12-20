package com.grameenphone.wipro.task_executor.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * to use log.dir variable in logback.xml from external property source
 */
public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {
	private boolean started = false;

	@Override
	public void start() {
		if (started) return;
		Context context = getContext();
		context.putProperty("log.dir", PropertyUtil.getProperty("log.dir"));
		context.putProperty("root.log.level", PropertyUtil.getProperty("root.log.level", "off"));
		System.setProperty("hibernate.log.level.sql", PropertyUtil.getProperty("hibernate.log.level.sql", "off"));
		System.setProperty("hibernate.log.level.param.binder", PropertyUtil.getProperty("hibernate.log.level.param.binder", "off"));
		System.setProperty("hibernate.log.level.param.extractor", PropertyUtil.getProperty("hibernate.log.level.param.extractor", "off"));
		String hikariLogPeriod = PropertyUtil.getProperty("com.zaxxer.hikari.housekeeping.periodMs", "15m");
		if(hikariLogPeriod != null) {
			System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "" + StringUtil.toMilli(hikariLogPeriod));
		}
		started = true;
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public boolean isResetResistant() {
		return true;
	}

	@Override
	public void onStart(LoggerContext context) {
	}

	@Override
	public void onReset(LoggerContext context) {
	}

	@Override
	public void onStop(LoggerContext context) {
	}

	@Override
	public void onLevelChange(Logger logger, Level level) {
	}
}