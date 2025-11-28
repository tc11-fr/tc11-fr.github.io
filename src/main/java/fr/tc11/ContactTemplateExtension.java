package fr.tc11;

import io.quarkus.arc.Unremovable;
import io.quarkus.qute.TemplateExtension;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;

/**
 * Qute template extension to expose contact email to templates.
 * 
 * Usage in templates: {contact:email}
 */
@TemplateExtension(namespace = "contact")
public class ContactTemplateExtension {

    /**
     * Returns the contact email address from configuration.
     * 
     * @return contact email address
     */
    public static String email() {
        return CDI.current().select(ContactConfig.class).get().getEmail();
    }

    @Singleton
    @Unremovable
    public static class ContactConfig {
        @ConfigProperty(name = "tc11.contact.email")
        String email;

        public String getEmail() {
            return email;
        }
    }
}
