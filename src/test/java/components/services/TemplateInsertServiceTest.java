package components.services;

import components.dto.InsertTemplateRequest;
import components.dto.InsertTemplateResponse;
import components.entities.BoardTemplate;
import components.entities.Userat;
import components.model.DrawingEvent;
import components.model.TemplateBounds;
import components.model.TemplateContent;
import components.model.TemplateEventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateInsertServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private RoomService roomService;
    @Mock
    private BoardStateService boardStateService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private TemplateInsertService templateInsertService;

    @BeforeEach
    void setUp() {
        templateInsertService = new TemplateInsertService(
                templateService,
                roomService,
                boardStateService,
                messagingTemplate
        );
    }

    @Test
    void insert_appliesOffsetScaleAndRegeneratesIds() {
        Userat teacher = new Userat();
        teacher.setId(1L);

        BoardTemplate template = new BoardTemplate();
        template.setId(42L);
        TemplateContent content = new TemplateContent();
        TemplateBounds bounds = new TemplateBounds();
        bounds.setWidth(100.0);
        bounds.setHeight(50.0);
        content.setBounds(bounds);

        DrawingEvent stroke = new DrawingEvent();
        stroke.setType("STROKE_START");
        stroke.setStrokeId("old-stroke");
        stroke.setX(10.0);
        stroke.setY(5.0);
        stroke.setWidth(2.0);
        content.setEvents(List.of(stroke));

        when(templateService.requireOwnedTemplate(teacher, 42L)).thenReturn(template);
        when(templateService.parseContent(template)).thenReturn(content);

        InsertTemplateRequest request = new InsertTemplateRequest();
        request.setX(100.0);
        request.setY(200.0);
        request.setScale(2.0);

        InsertTemplateResponse response = templateInsertService.insert(teacher, 7L, 42L, request);

        assertNotNull(response.getTemplateInstanceId());
        assertEquals(2, response.getEventCount());

        ArgumentCaptor<DrawingEvent> captor = ArgumentCaptor.forClass(DrawingEvent.class);
        verify(boardStateService, times(2)).addEvent(eq(7L), captor.capture());

        DrawingEvent groupCreate = captor.getAllValues().get(0);
        assertEquals(TemplateEventTypes.GROUP_CREATE, groupCreate.getType());
        assertEquals(100.0, groupCreate.getX());
        assertEquals(200.0, groupCreate.getY());
        assertEquals(200.0, groupCreate.getWidth());
        assertEquals(100.0, groupCreate.getHeight());

        DrawingEvent pasted = captor.getAllValues().get(1);
        assertEquals("STROKE_START", pasted.getType());
        assertEquals(120.0, pasted.getX());
        assertEquals(210.0, pasted.getY());
        assertEquals(4.0, pasted.getWidth());
        assertNotNull(pasted.getStrokeId());
        org.junit.jupiter.api.Assertions.assertNotEquals("old-stroke", pasted.getStrokeId());
        assertEquals(response.getTemplateInstanceId(), pasted.getTemplateInstanceId());
        assertEquals(42L, pasted.getSavedTemplateId());
    }

    @Test
    void insert_keepsStrokeIdStableAcrossMoveEvents() {
        Userat teacher = new Userat();
        teacher.setId(1L);

        BoardTemplate template = new BoardTemplate();
        template.setId(42L);
        TemplateContent content = new TemplateContent();
        TemplateBounds bounds = new TemplateBounds();
        bounds.setWidth(10.0);
        bounds.setHeight(10.0);
        content.setBounds(bounds);

        DrawingEvent start = new DrawingEvent();
        start.setType("STROKE_START");
        start.setStrokeId("same-stroke");
        start.setX(1.0);
        start.setY(1.0);

        DrawingEvent move = new DrawingEvent();
        move.setType("STROKE_MOVE");
        move.setStrokeId("same-stroke");
        move.setX(2.0);
        move.setY(3.0);

        content.setEvents(List.of(start, move));

        when(templateService.requireOwnedTemplate(teacher, 42L)).thenReturn(template);
        when(templateService.parseContent(template)).thenReturn(content);

        InsertTemplateRequest request = new InsertTemplateRequest();
        request.setX(0.0);
        request.setY(0.0);
        request.setScale(1.0);

        templateInsertService.insert(teacher, 7L, 42L, request);

        ArgumentCaptor<DrawingEvent> captor = ArgumentCaptor.forClass(DrawingEvent.class);
        verify(boardStateService, times(3)).addEvent(eq(7L), captor.capture());

        DrawingEvent pastedStart = captor.getAllValues().get(1);
        DrawingEvent pastedMove = captor.getAllValues().get(2);
        assertEquals(pastedStart.getStrokeId(), pastedMove.getStrokeId());
        org.junit.jupiter.api.Assertions.assertNotEquals("same-stroke", pastedStart.getStrokeId());
    }
}
