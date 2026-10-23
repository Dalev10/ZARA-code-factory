package com.codefactory.supplychain.qa.config;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("com/codefactory/supplychain/qa/features")
@ConfigurationParameter(key = "cucumber.glue",
        value = "com.codefactory.supplychain.qa.config,com.codefactory.supplychain.qa.steps")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty, html:target/cucumber-report.html")
public class CucumberIT {
}
