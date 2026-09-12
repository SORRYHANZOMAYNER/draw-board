package components.controllers;

import components.config.SecurityUtils;
import components.dto.CreateTemplateRequest;
import components.dto.TemplateDetailResponse;
import components.dto.TemplateSummaryResponse;
import components.dto.UpdateTemplateRequest;
import components.entities.Userat;
import components.services.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<TemplateSummaryResponse>> listMine() {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);
        return ResponseEntity.ok(templateService.listMine(teacher));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateDetailResponse> getById(@PathVariable Long id) {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);
        return ResponseEntity.ok(templateService.getById(teacher, id));
    }

    @PostMapping
    public ResponseEntity<TemplateDetailResponse> create(@RequestBody CreateTemplateRequest request) {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);
        TemplateDetailResponse created = templateService.create(teacher, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TemplateDetailResponse> rename(
            @PathVariable Long id,
            @RequestBody UpdateTemplateRequest request
    ) {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);
        return ResponseEntity.ok(templateService.rename(teacher, id, request.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);
        templateService.delete(teacher, id);
        return ResponseEntity.noContent().build();
    }
}
