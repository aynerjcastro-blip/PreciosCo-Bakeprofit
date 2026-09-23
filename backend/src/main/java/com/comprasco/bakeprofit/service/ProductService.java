package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.repository.ProductRepository;
import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.dto.ProductResponse;
import com.comprasco.bakeprofit.service.CategoryService;
import com.comprasco.bakeprofit.exception.ProductNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService (ProductRepository productRepository,
                           CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    } 

    public List<ProductResponse> findAll () {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public Product findById (Long id) {
        return productRepository.findById(id).
                orElseThrow(() -> new ProductNotFoundException("Producto no encontrado con id: " + id));
    }

    public ProductResponse findByIdResponse (Long id) {
        return ProductResponse.from(findById(id));
    }

    public List<ProductResponse> findActive () {
        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> findInactive () {
        return productRepository.findByActiveFalse().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> search (String name, Long idCategory) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasIdCategory = idCategory != null;

        Category category = new Category();
        if(hasIdCategory) category = categoryService.findById(idCategory);

        if(hasName && hasIdCategory) {
            return productRepository.findByNameContainingIgnoreCaseAndCategoryAndActiveTrue(name, category).stream()
                    .map(ProductResponse::from)
                    .toList();
        } else if(hasName) {
            return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name).stream()
                    .map(ProductResponse::from)
                    .toList();
        } else if(hasIdCategory) {
            return productRepository.findByCategoryAndActiveTrue(category).stream()
                    .map(ProductResponse::from)
                    .toList();
        }

        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse create (String name, String unit, Long idCategory) {
        Category category = categoryService.findById(idCategory);

        Product product = new Product();
        product.setName(name);
        product.setUnit(unit);
        product.setCategory(category);

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update (Long id, String name, String unit, Long idCategory) {
        Product product = findById(id);
        Category category = categoryService.findById(idCategory);

        product.setName(name);
        product.setUnit(unit);
        product.setCategory(category);

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void activateProduct (Long id) {
        Product product = findById(id);
        product.setActive(true);

        productRepository.save(product);
    }

    @Transactional
    public void deactivateProduct (Long id) {
        Product product = findById(id);
        product.setActive(false);

        productRepository.save(product);
    }
}
