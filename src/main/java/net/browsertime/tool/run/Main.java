/*******************************************************************************************************************************
 * It's Browser Time!
 * 
 * 
 * Copyright (C) 2013 by Tobias Lidskog (https://twitter.com/tobiaslidskog) & Peter Hedenskog
 * (http://peterhedenskog.com)
 * 
 ******************************************************************************************************************************** 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * 
 ******************************************************************************************************************************** 
 */
package net.browsertime.tool.run;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import net.browsertime.tool.BrowserTimeException;
import net.browsertime.tool.serializer.Serializer;
import net.browsertime.tool.serializer.SerializerFactory;
import net.browsertime.tool.timingrunner.TimingRunner;
import net.browsertime.tool.timings.TimingSession;
import net.browsertime.tool.webdriver.WebDriverValidationException;
import org.apache.commons.cli.ParseException;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 *
 */
public class Main {

  public static final int ERROR = 1;
  public static final int OK = 0;

  public static void main(String[] args) {
    Main app = new Main();
    int status = app.handleCommandLine(args);

    System.exit(status);
  }

  private static String getStackTrace(Exception e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    return stringWriter.toString();
  }

  int handleCommandLine(String[] args) {
    int commandStatus = OK;
    boolean shouldShowUsage = false;

    CliParser parser = new CliParser();

    try {
      parser.parseArgs(args);

      if (parser.shouldShowHelp()) {
        shouldShowUsage = true;
      } else if (parser.shouldShowVersion()) {
        System.out.println(Version.getVersion());
      } else {
        TimingConfig config = parser.parseTimingConfig();
        URL url = parser.parseUrl();
        run(url, config);
      }
    } catch (ParseException e) {
      commandStatus = ERROR;
      shouldShowUsage = true;
      printSyntaxError("Error parsing command line options: " + e.getMessage());
    } catch (IOException e) {
      commandStatus = ERROR;
      printSyntaxError("Error creating output file: " + e.getMessage());
    } catch (WebDriverValidationException e) {
      commandStatus = ERROR;
      printSyntaxError(e.getMessage());
    } catch (BrowserTimeException e) {
      commandStatus = ERROR;

      printSyntaxError("An unknown error occurred!\nPlease attach the following information to a bug report at "
          + "https://github.com/tobli/browsertime/issues");
      printSyntaxError(getStackTrace(e));
    }

    if (shouldShowUsage) {
      parser.printUsage();
    }

    return commandStatus;
  }

  private void run(URL url, TimingConfig config) throws IOException, BrowserTimeException {
    Injector injector = Guice.createInjector(GuiceSetup.setupModules(config));

    TimingRunner timingRunner = injector.getInstance(TimingRunner.class);
    SerializerFactory factory = injector.getInstance(SerializerFactory.class);
    Serializer serializer =
        factory.create(config.outputWriter, config.shouldPrettyPrint, config.shouldIncludeRuns);

    TimingSession session = timingRunner.run(url, config.numIterations, config.timeoutSeconds);
    serializer.serialize(session);
  }

  void printSyntaxError(String s) {
    System.err.println(s);
  }
}
