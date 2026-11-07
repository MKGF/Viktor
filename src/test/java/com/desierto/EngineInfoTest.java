package com.desierto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class EngineInfoTest {
  @Test
  void engineNameMatchesProjectVersion() throws Exception {
    Element project = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(Path.of("pom.xml").toFile()).getDocumentElement();
    String version = null;
    for (Node node = project.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && "version".equals(element.getTagName())) {
        version = element.getTextContent().trim();
        break;
      }
    }
    assertEquals("Viktor " + version, EngineInfo.name());
  }
}
