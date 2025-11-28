package fr.tc11;

import io.quarkus.qute.TemplateExtension;
import org.eclipse.microprofile.config.ConfigProvider;

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
        return ConfigProvider.getConfig()
                .getOptionalValue("tc11.contact.email", String.class)
                .orElse("tc11-assb@fft.fr");
    }
}
