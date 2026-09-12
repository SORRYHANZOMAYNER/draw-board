package components.services;

import components.config.SecurityUtils;
import components.entities.Userat;
import components.model.DrawingEvent;
import components.model.TemplateEventTypes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TemplateEventGuard {

    public void requireTeacherForGroupManipulation(DrawingEvent event) {
        if (!TemplateEventTypes.isTeacherOnly(event.getType())) {
            return;
        }
        if (event.getTemplateInstanceId() == null || event.getTemplateInstanceId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateInstanceId обязателен");
        }
        Userat user = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(user);
    }
}
