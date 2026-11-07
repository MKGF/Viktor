package com.desierto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public final class EngineInfo {
  private static final String VERSION = loadVersion();

  private EngineInfo() {
  }

  public static String name() {
    return "Viktor " + VERSION;
  }

  private static String loadVersion() {
    try (InputStream input = EngineInfo.class.getResourceAsStream("/viktor-version.properties")) {
      if (input == null) {
        throw new IllegalStateException("Missing Viktor version resource");
      }
      Properties properties = new Properties();
      properties.load(input);
      String version = properties.getProperty("version");
      if ("${project.version}".equals(version)) {
        version = readProjectVersion();
      }
      if (version == null || !version.matches("[0-9A-Za-z][0-9A-Za-z._-]*")) {
        throw new IllegalStateException("Invalid Viktor version in version resource: " + version);
      }
      return version;
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot read Viktor version resource", ex);
    }
  }

  private static String readProjectVersion() {
    try (InputStream input = Files.newInputStream(Path.of("pom.xml"))) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setNamespaceAware(true);
      Element project = factory.newDocumentBuilder().parse(input).getDocumentElement();
      for (Node node = project.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node instanceof Element element && "version".equals(element.getLocalName())) {
          return element.getTextContent().trim();
        }
      }
      throw new IllegalStateException("Missing project version in pom.xml");
    } catch (IOException | ParserConfigurationException | SAXException ex) {
      throw new IllegalStateException("Cannot read project version from pom.xml", ex);
    }
  }
}
