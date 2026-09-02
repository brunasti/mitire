package it.brunasti.mitire.ui.views;

import com.vaadin.flow.component.Html;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ManualViewSmokeTest {

    @Test
    void manualMarkdownRendersToValidSingleRootHtml() throws Exception {
        String markdown;
        try (InputStream in = new ClassPathResource("manual/user-manual.md").getInputStream()) {
            markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(markdown).contains("Administrator");

        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        String bodyHtml = HtmlRenderer.builder().build().render(document);
        assertThat(bodyHtml).contains("<h1>MiTiRe User Manual</h1>");

        // Exercises ManualView's exact wrapping (style tag + content div, all inside
        // one outer root) - this is what would throw if the rendered markdown weren't
        // well-formed HTML for Vaadin's Html component to parse.
        ManualView view = new ManualView();
        assertThat(view.getChildren().findFirst()).isPresent();
        Html html = (Html) view.getChildren().findFirst().orElseThrow();
        assertThat(html.getInnerHtml()).contains("MiTiRe User Manual");
    }
}
