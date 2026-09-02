package it.brunasti.mitire.ui.views;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Route("manual")
@PageTitle("User Manual | MiTiRe")
@PermitAll
public class ManualView extends VerticalLayout {

    public ManualView() {
        setSizeFull();
        getStyle().set("overflow", "auto");
        add(new Html("<div>" + wrapWithStyle(renderManualHtml()) + "</div>"));
    }

    private String wrapWithStyle(String bodyHtml) {
        return """
                <style>
                  .mitire-manual {
                    max-width: 860px;
                    margin: 0 auto;
                    padding: 2rem;
                    line-height: 1.6;
                    font-family: var(--lumo-font-family);
                    color: var(--lumo-body-text-color);
                  }
                  .mitire-manual h1, .mitire-manual h2, .mitire-manual h3 {
                    color: var(--lumo-header-text-color);
                    margin-top: 2rem;
                  }
                  .mitire-manual code {
                    background: var(--lumo-contrast-10pct);
                    padding: 0.1rem 0.3rem;
                    border-radius: var(--lumo-border-radius-s);
                  }
                  .mitire-manual blockquote {
                    border-left: 4px solid var(--lumo-primary-color);
                    margin-left: 0;
                    padding-left: 1rem;
                    color: var(--lumo-secondary-text-color);
                  }
                  .mitire-manual hr {
                    border: none;
                    border-top: 1px solid var(--lumo-contrast-20pct);
                    margin: 2rem 0;
                  }
                </style>
                <div class="mitire-manual">
                """ + bodyHtml + "</div>";
    }

    private String renderManualHtml() {
        try (InputStream in = new ClassPathResource("manual/user-manual.md").getInputStream()) {
            String markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdown);
            return HtmlRenderer.builder().build().render(document);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the user manual", e);
        }
    }
}
