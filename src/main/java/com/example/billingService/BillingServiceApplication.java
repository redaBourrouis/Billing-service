package com.example.billingService;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Date billingDate;
    private Long customersId;
    @Transient
    private Customer customer;
    @OneToMany(mappedBy = "bill")
    private Collection<ProductItem> productItems;
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
class ProductItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    @Transient
    private Product product;
    private double price;
    private double quantity;
    @ManyToOne
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Bill bill;

}

@RepositoryRestResource
interface ProductRepo extends JpaRepository<ProductItem, Long> {

}

@RepositoryRestResource
interface BillingRepo extends JpaRepository<Bill, Long> {

}

@RepositoryRestResource
interface ProductItemsRepo extends JpaRepository<ProductItem, Long> {

}

@Projection(name = "fullBilling", types = Bill.class)
interface BillProjection {
    public Long getId();

    public Date getBillingDate();

    public Long getCustomersId();

    public Collection<ProductItem> getProductItems();

}

@Data
class Customer {
    private Long id;
    private String name;
    private String email;
}

@FeignClient(name = "CUSTOMER")
interface CustomerService {
    @GetMapping("/customers/{id}")
    public Customer findCustomerById(@PathVariable(name = "id") Long id);

}

@Data
class Product {
    private Long id;
    private String name;
    private double price;
}

@FeignClient(name = "INVENTORYSERVICES")
interface ProductService {
    @GetMapping("/products/{id}")
    public Product findProductById(@PathVariable(name = "id") Long id);

    @GetMapping("/products")
    public PagedModel<Product> findAllProducts();
}


@SpringBootApplication
@EnableFeignClients
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }


    @Bean
    CommandLineRunner start(ProductRepo productRepo, BillingRepo billingRepo, CustomerService customerService, ProductService productService) {
        return args -> {
            Customer c2 = customerService.findCustomerById(1L);
            System.out.println("******************");
            System.out.println("ID est " + c2.getId());
            System.out.println("nom est " + c2.getName());
            System.out.println("email est " + c2.getEmail());
            System.out.println("******************");
            Bill bill1 = billingRepo.save(new Bill(null, new Date(), c2.getId(), null, null));

            PagedModel<Product> products = productService.findAllProducts();
            products.getContent().forEach(p -> {
                productRepo.save(new ProductItem(null, p.getId(), null, p.getPrice(), 100 * Math.random() + 999, bill1));
            });

       /* Product p1=productService.findProductById(1L);
			System.out.println("******************");
			System.out.println("ID est"+ p1.getId());
			System.out.println("nom est"+ p1.getName());
			System.out.println("prix est"+ p1.getPrice());
			System.out.println("******************");
			productRepo.save(new ProductItem(null,p1.getId(),p1.getPrice(),65,bill1));
			Product p2=productService.findProductById(2L);
			System.out.println("******************");
			System.out.println("ID est"+ p2.getId());
			System.out.println("nom est"+ p2.getName());
			System.out.println("prix est"+ p2.getPrice());
			System.out.println("******************");
			productRepo.save(new ProductItem(null,p1.getId(),p2.getPrice(),100,bill1));
			Product p3=productService.findProductById(3L);
			System.out.println("******************");
			System.out.println("ID est"+ p3.getId());
			System.out.println("nom est"+ p3.getName());
			System.out.println("prix est"+ p3.getPrice());
			System.out.println("******************");
			productRepo.save(new ProductItem(null,p1.getId(),p3.getPrice(),49,bill1));*/
        };

    }
}

@RestController
class BillingController {
    @Autowired
    private BillingRepo billingRepo;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductItemsRepo productItemsRepo;

    @GetMapping(value = "/fullBill/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Bill getBill(@PathVariable(name = "id") Long id) {
        Bill bill = billingRepo.findById(id).get();
        bill.setCustomer(customerService.findCustomerById(bill.getCustomersId()));
        bill.getProductItems().forEach(pi -> {
            pi.setProduct(productService.findProductById(pi.getProductId()));
        });
        return bill;
    }
}

