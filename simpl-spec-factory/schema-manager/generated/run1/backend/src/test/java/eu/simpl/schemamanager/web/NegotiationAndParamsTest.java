package eu.simpl.schemamanager.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.schemamanager.domain.SchemaType;
import org.junit.jupiter.api.Test;

class NegotiationAndParamsTest {

    @Test
    void jsonAcceptRules() {
        Negotiation.requireJson(null);
        Negotiation.requireJson("application/json");
        Negotiation.requireJson("application/problem+json");
        Negotiation.requireJson("*/*");
        Negotiation.requireJson("text/html, application/*;q=0.5");
        assertThatThrownBy(() -> Negotiation.requireJson("text/html")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Negotiation.requireJson("application/xml")).isInstanceOf(ApiException.class);
    }

    @Test
    void contentAcceptRules() {
        assertThat(Negotiation.content(null, true)).isEqualTo(Negotiation.ContentChoice.CONTENT);
        assertThat(Negotiation.content("text/turtle", true)).isEqualTo(Negotiation.ContentChoice.CONTENT);
        assertThat(Negotiation.content("*/*", true)).isEqualTo(Negotiation.ContentChoice.CONTENT);
        assertThat(Negotiation.content("application/json", true)).isEqualTo(Negotiation.ContentChoice.METADATA);
        assertThatThrownBy(() -> Negotiation.content("application/json", false)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Negotiation.content("application/xml", true)).isInstanceOf(ApiException.class);
    }

    @Test
    void mediaTypeChecks() {
        Negotiation.requireMultipart("multipart/form-data; boundary=x");
        Negotiation.requireJsonBody("application/json;charset=UTF-8");
        assertThatThrownBy(() -> Negotiation.requireMultipart("application/json")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Negotiation.requireJsonBody("text/plain")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Negotiation.requireJsonBody(null)).isInstanceOf(ApiException.class);
    }

    @Test
    void nameRules() {
        assertThat(Params.isPascal("DataAsset")).isTrue();
        assertThat(Params.isPascal("Ab")).isFalse();
        assertThat(Params.isPascal("dataAsset")).isFalse();
        assertThat(Params.isPascal("Infrastructure$chema!")).isFalse();
        assertThat(Params.isPascal("A".repeat(65))).isFalse();
        assertThat(Params.isPascal("Application Shape")).isFalse();
    }

    @Test
    void queryParameterRules() {
        assertThat(Params.schemaType(null, "q")).isEqualTo(SchemaType.RESOURCE);
        assertThatThrownBy(() -> Params.schemaType("resource", "q")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Params.status("INVALID_STATUS")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Params.resourceTypeFilter("Data", SchemaType.SEARCH, "q")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Params.resourceTypeFilter("data", SchemaType.RESOURCE, "q")).isInstanceOf(ApiException.class);
        assertThat(Params.page(null)).isEqualTo(1);
        assertThat(Params.pageSize(null)).isEqualTo(20);
        assertThatThrownBy(() -> Params.page("0")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Params.pageSize("101")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Params.page("x")).isInstanceOf(ApiException.class);
        assertThat(Params.version("12")).isEqualTo(12);
        assertThat(Params.version("9.9.9")).isNull();
        assertThat(Params.version("0")).isNull();
    }

    @Test
    void webhookUrlRules() {
        assertThat(WebhookController.isHttpUrl("https://api.governance.com/listen")).isTrue();
        assertThat(WebhookController.isHttpUrl("http://host.docker.internal:9999/hooks/x")).isTrue();
        assertThat(WebhookController.isHttpUrl("url")).isFalse();
        assertThat(WebhookController.isHttpUrl("not-a-url")).isFalse();
        assertThat(WebhookController.isHttpUrl("http://")).isFalse();
        assertThat(WebhookController.isHttpUrl("ftp://invalid.com")).isFalse();
    }
}
