/*
 * Copyright 2004 - 2012 Mirko Nasato and contributors
 *           2016 - 2017 Simon Braconnier and contributors
 *
 * This file is part of JODConverter - Java OpenDocument Converter.
 *
 * JODConverter is an Open Source software: you can redistribute it and/or
 * modify it under the terms of either (at your option) of the following
 * licenses:
 *
 * 1. The GNU Lesser General Public License v3 (or later)
 *    http://www.gnu.org/licenses/lgpl-3.0.txt
 * 2. The Apache License, Version 2.0
 *    http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package org.artofsolving.jodconverter.office;

import java.io.File;
import java.util.Map;

import com.sun.star.beans.PropertyValue;

import org.artofsolving.jodconverter.process.MacProcessManager;
import org.artofsolving.jodconverter.process.ProcessManager;
import org.artofsolving.jodconverter.process.PureJavaProcessManager;
import org.artofsolving.jodconverter.process.UnixProcessManager;
import org.artofsolving.jodconverter.process.WindowsProcessManager;
import org.artofsolving.jodconverter.util.PlatformUtils;

public final class OfficeUtils {

  private static final String EXECUTABLE_DEFAULT = "program/soffice.bin";
  private static final String EXECUTABLE_MAC = "program/soffice";
  private static final String EXECUTABLE_MAC_41 = "MacOS/soffice.bin";
  private static final String EXECUTABLE_WINDOWS = "program/soffice.exe";

  /**
   * Find the best process manager that will be used to retrieve a process PID and to kill a process
   * by PID.
   *
   * @return the best process manager according to the current OS.
   */
  public static ProcessManager findBestProcessManager() {

    if (PlatformUtils.isLinux()) {
      return new UnixProcessManager();
    } else if (PlatformUtils.isMac()) {
      return new MacProcessManager();
    } else if (PlatformUtils.isWindows()) {
      WindowsProcessManager windowsProcessManager = new WindowsProcessManager();
      return windowsProcessManager.isUsable()
          ? windowsProcessManager
          : new PureJavaProcessManager();
    } else {
      // NOTE: UnixProcessManager can't be trusted to work on Solaris
      // because of the 80-char limit on ps output there
      return new PureJavaProcessManager();
    }
  }

  private static File findOfficeHome(final String executablePath, final String[] homePaths) {

    for (final String homePath : homePaths) {
      final File homeDir = new File(homePath);
      if (new File(homeDir, executablePath).isFile()) {
        return homeDir;
      }
    }
    return null;
  }

  /**
   * Gets the default office home directory.
   *
   * @return A instance file to the directory which is the office home directory.
   */
  public static File getDefaultOfficeHome() {

    if (System.getProperty("office.home") != null) {
      return new File(System.getProperty("office.home"));
    }

    if (PlatformUtils.isWindows()) {

      // Try to find the most recent version of LibreOffice or OpenOffice,
      // starting with the 64-bit version. %ProgramFiles(x86)% on 64-bit
      // machines; %ProgramFiles% on 32-bit ones
      final String programFiles64 = System.getenv("ProgramFiles");
      final String programFiles32 = System.getenv("ProgramFiles(x86)");
      //@formatter:off
      return findOfficeHome(
          EXECUTABLE_WINDOWS,
          new String[] {
            programFiles64 + File.separator + "LibreOffice 5",
            programFiles64 + File.separator + "LibreOffice 4",
            programFiles64 + File.separator + "LibreOffice 3",
            programFiles32 + File.separator + "LibreOffice 5",
            programFiles32 + File.separator + "LibreOffice 4",
            programFiles32 + File.separator + "LibreOffice 3",
            programFiles32 + File.separator + "OpenOffice.org 3",
            programFiles32 + File.separator + "OpenOffice 4"
          });
      //@formatter:on

    } else if (PlatformUtils.isMac()) {

      //@formatter:off
      File homeDir =
          findOfficeHome(
              EXECUTABLE_MAC_41,
              new String[] {
                "/Applications/LibreOffice.app/Contents",
                "/Applications/OpenOffice.org.app/Contents"
              });
      //@formatter:on

      if (homeDir == null) {
        //@formatter:off
        homeDir =
            findOfficeHome(
                EXECUTABLE_MAC,
                new String[] {
                  "/Applications/LibreOffice.app/Contents",
                  "/Applications/OpenOffice.org.app/Contents"
                });
        //@formatter:on
      }

      return homeDir;
    } else {

      // Linux or other *nix variants
      //@formatter:off
      return findOfficeHome(
          EXECUTABLE_DEFAULT,
          new String[] {
            "/usr/lib64/libreoffice",
            "/usr/lib/libreoffice",
            "/opt/libreoffice",
            "/usr/lib64/openoffice",
            "/usr/lib64/openoffice.org3",
            "/usr/lib64/openoffice.org",
            "/usr/lib/openoffice",
            "/usr/lib/openoffice.org3",
            "/usr/lib/openoffice.org",
            "/opt/openoffice.org3"
          });
      //@formatter:on
    }
  }

  /**
   * Gets the office executable within an office installation.
   *
   * @param officeHome the root (home) directory of the office installation.
   * @return A instance of the executable file.
   */
  public static File getOfficeExecutable(final File officeHome) {

    // Mac
    if (PlatformUtils.isMac()) {
      return getOfficeExecutableMac(officeHome);
    }

    // Windows
    if (PlatformUtils.isWindows()) {
      return getOfficeExecutableWindows(officeHome);
    }

    // Everything else
    return getOfficeExecutableDefault(officeHome);
  }

  // Get the default office executable within an office installation
  private static File getOfficeExecutableDefault(final File officeHome) {
    return new File(officeHome, EXECUTABLE_DEFAULT);
  }

  // Get the office executable for a Mac OS within an office installation
  private static File getOfficeExecutableMac(final File officeHome) {

    // Starting with LibreOffice 4.1 the location of the executable has changed on Mac.
    // It's now in program/soffice. Handle both cases!
    File executableFile = new File(officeHome, EXECUTABLE_MAC_41);
    if (!executableFile.isFile()) {
      executableFile = new File(officeHome, EXECUTABLE_MAC);
    }
    return executableFile;
  }

  // Get the office executable for a Windows OS within an office installation
  private static File getOfficeExecutableWindows(final File officeHome) {

    return new File(officeHome, EXECUTABLE_WINDOWS);
  }

  /**
   * Creates a {@code PropertyValue} with the specified name and value.
   *
   * @param name the property name.
   * @param value the property value.
   * @return the created {@code PropertyValue}.
   */
  public static PropertyValue property(final String name, final Object value) {

    final PropertyValue prop = new PropertyValue(); // NOSONAR
    prop.Name = name;
    prop.Value = value;
    return prop;
  }

  /**
   * Converts a regular java map to an array of {@code PropertyValue}, usable as arguments with UNO
   * interface types.
   *
   * @param properties the map to convert.
   * @return an array of {@code PropertyValue}.
   */
  public static PropertyValue[] toUnoProperties(final Map<String, ?> properties) {

    final PropertyValue[] propertyValues = new PropertyValue[properties.size()];
    int i = 0;
    for (final Map.Entry<String, ?> entry : properties.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> subProperties = (Map<String, Object>) value;
        value = toUnoProperties(subProperties);
      }
      propertyValues[i++] = property((String) entry.getKey(), value);
    }
    return propertyValues;
  }

  /**
   * Constructs an URL from the specified file as expected by office.
   *
   * @param file the file for which an URL will be constructed.
   * @return a valid office URL.
   */
  public static String toUrl(final File file) {

    final String path = file.toURI().getRawPath();
    final String url = path.startsWith("//") ? "file:" + path : "file://" + path;
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  /**
   * Validates that the specified File instance is a valid office home directory.
   *
   * @param officeHome the home to validate.
   * @exception IllegalStateException if the specified directory if not a valid office home
   *     directory.
   */
  public static void validateOfficeHome(final File officeHome) {

    if (officeHome == null) {
      throw new IllegalStateException("officeHome not set and could not be auto-detected");
    } else if (!officeHome.isDirectory()) {
      throw new IllegalStateException(
          "officeHome doesn't exist or is not a directory: " + officeHome);
    } else if (!getOfficeExecutable(officeHome).isFile()) {
      throw new IllegalStateException(
          "Invalid officeHome: it doesn't contain soffice.bin: " + officeHome);
    }
  }

  /**
   * Validates that the specified File instance is a valid office template profile directory.
   *
   * @param templateProfileDir the directory to validate.
   * @exception IllegalStateException if the specified directory if not a valid office template
   *     profile directory.
   */
  public static void validateOfficeTemplateProfileDirectory(final File templateProfileDir) {

    // Template profile directory is not required.
    if (templateProfileDir == null || new File(templateProfileDir, "user").isDirectory()) {
      return;
    }

    throw new IllegalStateException(
        "templateProfileDir doesn't appear to contain a user profile: " + templateProfileDir);
  }

  /**
   * Validates that the specified File instance is a valid office working directory.
   *
   * @param workingDir the directory to validate.
   * @exception IllegalStateException if the specified directory if not a valid office working
   *     directory.
   */
  public static void validateOfficeWorkingDirectory(final File workingDir) {

    if (!workingDir.isDirectory()) {
      throw new IllegalStateException(
          "workingDir doesn't exist or is not a directory: " + workingDir);
    }
  }

  // Private ctor.
  private OfficeUtils() { // NOSONAR
    throw new AssertionError("utility class must not be instantiated");
  }
}
