package mz.ebooks.commerce;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "ebooks-commerce-service",
        version = "1.0",
        description = "Orders, payments (M-Pesa/E-mola/Stripe/PayPal) and subscriptions",
        contact = @Contact(name = "ebooks.co.mz", email = "api@ebooks.co.mz")
))
public class EbooksCommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EbooksCommerceApplication.class, args);
    }
}
