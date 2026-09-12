package components.services;

import components.dto.CreateTemplateRequest;
import components.dto.TemplateDetailResponse;
import components.dto.TemplateSummaryResponse;
import components.entities.BoardTemplate;
import components.entities.Userat;
import components.model.DrawingEvent;
import components.model.TemplateBounds;
import components.model.TemplateContent;
import components.repositories.BoardTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.List;

@Service
public class TemplateService {

    public static final int MAX_TEMPLATES_PER_TEACHER = 100;
    public static final int MAX_EVENTS_PER_TEMPLATE = 2000;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_CONTENT_CHARS = 5_000_000;
    public static final int MAX_PREVIEW_CHARS = 2_000_000;

    private final BoardTemplateRepository boardTemplateRepository;
    private final ObjectMapper objectMapper;

    public TemplateService(BoardTemplateRepository boardTemplateRepository, ObjectMapper objectMapper) {
        this.boardTemplateRepository = boardTemplateRepository;
        this.objectMapper = objectMapper;
    }

    public List<TemplateSummaryResponse> listMine(Userat teacher) {
        return boardTemplateRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

    public TemplateDetailResponse getById(Userat teacher, Long templateId) {
        BoardTemplate entity = requireOwnedTemplate(teacher, templateId);
        return toDetail(entity);
    }

    public TemplateDetailResponse create(Userat teacher, CreateTemplateRequest request) {
        validateCreateRequest(teacher, request);

        TemplateContent content = new TemplateContent();
        content.setVersion(TemplateContent.CURRENT_VERSION);
        content.setBounds(request.getBounds());
        content.setEvents(sanitizeEventsForStorage(request.getEvents()));

        String contentJson = serializeContent(content);

        BoardTemplate entity = new BoardTemplate();
        entity.setTeacherId(teacher.getId());
        entity.setName(request.getName().trim());
        entity.setContent(contentJson);
        entity.setPreviewData(trimPreview(request.getPreviewData()));
        entity.setCreatedAt(new Date());

        return toDetail(boardTemplateRepository.save(entity));
    }

    public TemplateDetailResponse rename(Userat teacher, Long templateId, String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название шаблона не может быть пустым");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название слишком длинное");
        }

        BoardTemplate entity = requireOwnedTemplate(teacher, templateId);
        entity.setName(trimmed);
        entity.setUpdatedAt(new Date());
        return toDetail(boardTemplateRepository.save(entity));
    }

    public void delete(Userat teacher, Long templateId) {
        BoardTemplate entity = requireOwnedTemplate(teacher, templateId);
        boardTemplateRepository.delete(entity);
    }

    public BoardTemplate requireOwnedTemplate(Userat teacher, Long templateId) {
        return boardTemplateRepository.findByIdAndTeacherId(templateId, teacher.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Шаблон не найден"));
    }

    public TemplateContent parseContent(BoardTemplate entity) {
        try {
            return objectMapper.readValue(entity.getContent(), TemplateContent.class);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось прочитать шаблон");
        }
    }

    private void validateCreateRequest(Userat teacher, CreateTemplateRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название шаблона обязательно");
        }
        if (request.getName().trim().length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название слишком длинное");
        }
        if (request.getBounds() == null
                || request.getBounds().getWidth() == null
                || request.getBounds().getHeight() == null
                || request.getBounds().getWidth() <= 0
                || request.getBounds().getHeight() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректные размеры области шаблона");
        }
        if (request.getEvents() == null || request.getEvents().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Шаблон должен содержать хотя бы одно событие");
        }
        if (request.getEvents().size() > MAX_EVENTS_PER_TEMPLATE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Слишком много объектов в шаблоне");
        }
        long count = boardTemplateRepository.countByTeacherId(teacher.getId());
        if (count >= MAX_TEMPLATES_PER_TEACHER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Достигнут лимит шаблонов (" + MAX_TEMPLATES_PER_TEACHER + ")"
            );
        }
        if (request.getPreviewData() != null && request.getPreviewData().length() > MAX_PREVIEW_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Превью слишком большое");
        }

        TemplateContent draft = new TemplateContent();
        draft.setVersion(TemplateContent.CURRENT_VERSION);
        draft.setBounds(request.getBounds());
        draft.setEvents(sanitizeEventsForStorage(request.getEvents()));
        String json = serializeContent(draft);
        if (json.length() > MAX_CONTENT_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Шаблон слишком большой");
        }
    }

    private List<DrawingEvent> sanitizeEventsForStorage(List<DrawingEvent> events) {
        return events.stream().map(event -> {
            DrawingEvent copy = new DrawingEvent();
            copy.setType(event.getType());
            copy.setStrokeId(event.getStrokeId());
            copy.setStickerId(event.getStickerId());
            copy.setText(event.getText());
            copy.setX(event.getX());
            copy.setY(event.getY());
            copy.setColor(event.getColor());
            copy.setWidth(event.getWidth());
            copy.setHeight(event.getHeight());
            copy.setImageId(event.getImageId());
            copy.setData(event.getData());
            copy.setImageWidth(event.getImageWidth());
            copy.setImageHeight(event.getImageHeight());
            copy.setShapeId(event.getShapeId());
            copy.setShapeType(event.getShapeType());
            copy.setStrokeWidth(event.getStrokeWidth());
            copy.setTextId(event.getTextId());
            copy.setFontSize(event.getFontSize());
            copy.setLocked(event.getLocked());
            return copy;
        }).toList();
    }

    private String serializeContent(TemplateContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректные данные шаблона");
        }
    }

    private String trimPreview(String previewData) {
        if (previewData == null || previewData.isBlank()) {
            return null;
        }
        return previewData;
    }

    private TemplateSummaryResponse toSummary(BoardTemplate entity) {
        TemplateBounds bounds = parseContent(entity).getBounds();
        return new TemplateSummaryResponse(
                entity.getId(),
                entity.getName(),
                bounds,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPreviewData() != null && !entity.getPreviewData().isBlank()
        );
    }

    private TemplateDetailResponse toDetail(BoardTemplate entity) {
        return new TemplateDetailResponse(
                entity.getId(),
                entity.getName(),
                parseContent(entity),
                entity.getPreviewData(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
