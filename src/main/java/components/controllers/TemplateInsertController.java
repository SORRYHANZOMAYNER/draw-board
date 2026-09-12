package components.controllers;

import components.config.SecurityUtils;
import components.dto.InsertTemplateRequest;
import components.dto.InsertTemplateResponse;
import components.entities.Userat;
import components.services.TemplateInsertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/room/{roomId}/templates")
public class TemplateInsertController {

    private final TemplateInsertService templateInsertService;

    public TemplateInsertController(TemplateInsertService templateInsertService) {
        this.templateInsertService = templateInsertService;
    }

    @PostMapping("/{templateId}/insert")
    public ResponseEntity<InsertTemplateResponse> insert(
            @PathVariable Long roomId,
            @PathVariable Long templateId,
            @RequestBody InsertTemplateRequest request
    ) {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);
        InsertTemplateResponse response = templateInsertService.insert(teacher, roomId, templateId, request);
        return ResponseEntity.ok(response);
    }
}
