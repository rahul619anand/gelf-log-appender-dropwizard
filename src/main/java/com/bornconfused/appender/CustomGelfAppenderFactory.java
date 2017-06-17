
package com.bornconfused.appender;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.bornconfused.logbackgelf.GelfLayout;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import io.dropwizard.logging.AbstractAppenderFactory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeName("gelf-console")
public class CustomGelfAppenderFactory extends AbstractAppenderFactory {
  public enum ConsoleStream {
    STDOUT("System.out"), STDERR("System.err");

    private final String value;

    ConsoleStream(String value) {
      this.value = value;
    }

    public String get() {
      return value;
    }
  }

  private boolean loggerNameEnabled, threadNameEnabled;
  private TimeZone timeZone = TimeZone.getTimeZone("UTC");
  @NotNull
  private ConsoleStream target = ConsoleStream.STDOUT;

  @Override
  public Appender<ILoggingEvent> build(LoggerContext context, String applicationName, Layout<ILoggingEvent> layout) {

    final ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    appender.setName("gelf-appender");
    appender.setContext(context);
    appender.setTarget(target.get());

    LayoutWrappingEncoder<ILoggingEvent> layoutEncoder = new LayoutWrappingEncoder<>();
    layoutEncoder.setLayout(buildGelfLayout(context));
    appender.setEncoder(layoutEncoder);

    addThresholdFilter(appender, threshold);
    appender.start();

    return wrapAsync(appender);
  }

  private Layout<ILoggingEvent> buildGelfLayout(LoggerContext context) {
    GelfLayout<ILoggingEvent> gelfLayout = new GelfLayout<>();
    Map<String, String> additionalFieldMap = new HashMap<>();
    additionalFieldMap.put("Correlation-Id", "_Correlation-Id");
    gelfLayout.setAdditionalFields(additionalFieldMap);
    gelfLayout.setUseThreadName(isThreadNameEnabled());
    gelfLayout.setUseLoggerName(isLoggerNameEnabled());
    gelfLayout.setContext(context);
    gelfLayout.start();
    return gelfLayout;
  }

}
