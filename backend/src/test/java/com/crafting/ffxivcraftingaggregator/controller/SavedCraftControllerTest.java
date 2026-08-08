package com.crafting.ffxivcraftingaggregator.controller;

import com.crafting.ffxivcraftingaggregator.domain.dto.CreateSavedCraftRequest;
import com.crafting.ffxivcraftingaggregator.domain.dto.SavedCraftDto;
import com.crafting.ffxivcraftingaggregator.domain.dto.SavedCraftRecipeRequest;
import com.crafting.ffxivcraftingaggregator.domain.entity.Role;
import com.crafting.ffxivcraftingaggregator.domain.entity.User;
import com.crafting.ffxivcraftingaggregator.exception.SavedCraftNotFoundException;
import com.crafting.ffxivcraftingaggregator.exception.UnknownWorldException;
import com.crafting.ffxivcraftingaggregator.exception.WorldDataCenterMismatchException;
import com.crafting.ffxivcraftingaggregator.security.FfxivUserDetails;
import com.crafting.ffxivcraftingaggregator.service.SavedCraftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for SavedCraftController.
 *
 * <p>The service is mocked, so these cover ONLY what the controller is responsible for: routing,
 * request binding, bean validation, the authenticated user's id reaching the service, and
 * exceptions being translated into the right status. Whether a craft is persisted correctly is
 * the service's job and belongs in a service test with a real database.
 *
 * <p>Built through the full Spring context rather than @WebMvcTest so the real SecurityFilterChain
 * runs. Binding to the controller alone would skip JwtAuthFilter entirely and every endpoint would
 * look secured-but-passing - a false negative worth avoiding.
 */
@SpringBootTest
@DisplayName("SavedCraftController")
class SavedCraftControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SavedCraftService savedCraftService;

    private MockMvc mockMvc;

    static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CRAFT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RECIPE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("rejects an unauthenticated request without touching the service")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/saved-crafts"))
                .andExpect(status().isUnauthorized());

        // The important half: a rejected request must not reach business logic at all.
        verifyNoInteractions(savedCraftService);
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/saved-crafts")
    class Create {

        @Test
        @WithTestUser
        @DisplayName("passes the authenticated user's id, never anything from the request body")
        void usesAuthenticatedUserId() throws Exception {
            when(savedCraftService.createSavedCraftRequest(any(), any())).thenReturn(sampleDto());

            mockMvc.perform(post("/api/v1/saved-crafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isOk());

            // If the controller ever took a userId from the payload, one user could create crafts
            // owned by another. This pins that it comes from the security context.
            verify(savedCraftService).createSavedCraftRequest(eq(USER_ID), any());
        }

        @Test
        @WithTestUser
        @DisplayName("binds recipe quantities from the request body")
        void bindsRecipeQuantities() throws Exception {
            when(savedCraftService.createSavedCraftRequest(any(), any())).thenReturn(sampleDto());

            CreateSavedCraftRequest request = CreateSavedCraftRequest.builder()
                    .title("Gunblade run")
                    .dataCenter("Aether")
                    .world("Faerie")
                    .recipes(List.of(new SavedCraftRecipeRequest(RECIPE_ID, 3)))
                    .build();

            mockMvc.perform(post("/api/v1/saved-crafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            ArgumentCaptor<CreateSavedCraftRequest> captor =
                    ArgumentCaptor.forClass(CreateSavedCraftRequest.class);
            verify(savedCraftService).createSavedCraftRequest(eq(USER_ID), captor.capture());

            assertThat(captor.getValue().recipes()).singleElement().satisfies(line -> {
                assertThat(line.recipeId()).isEqualTo(RECIPE_ID);
                assertThat(line.quantity()).isEqualTo(3);
            });
        }

        @Test
        @WithTestUser
        @DisplayName("accepts a request with no world - meaning price across the whole data centre")
        void worldIsOptional() throws Exception {
            when(savedCraftService.createSavedCraftRequest(any(), any())).thenReturn(sampleDto());

            CreateSavedCraftRequest request = CreateSavedCraftRequest.builder()
                    .title("DC wide")
                    .dataCenter("Aether")
                    .world(null)
                    .recipes(List.of(new SavedCraftRecipeRequest(RECIPE_ID, 1)))
                    .build();

            mockMvc.perform(post("/api/v1/saved-crafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithTestUser
        @DisplayName("rejects a blank title with 400 and does not call the service")
        void blankTitleIsRejected() throws Exception {
            CreateSavedCraftRequest request = CreateSavedCraftRequest.builder()
                    .title("  ")
                    .dataCenter("Aether")
                    .recipes(List.of(new SavedCraftRecipeRequest(RECIPE_ID, 1)))
                    .build();

            mockMvc.perform(post("/api/v1/saved-crafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(savedCraftService);
        }

        @Test
        @WithTestUser
        @DisplayName("rejects a quantity below 1")
        void zeroQuantityIsRejected() throws Exception {
            // A zero-quantity line would price at 0 gil and read downstream as free, so the
            // @Min(1) on SavedCraftRecipeRequest has to fire through @Valid on the list element.
            CreateSavedCraftRequest request = CreateSavedCraftRequest.builder()
                    .title("Bad quantity")
                    .dataCenter("Aether")
                    .recipes(List.of(new SavedCraftRecipeRequest(RECIPE_ID, 0)))
                    .build();

            mockMvc.perform(post("/api/v1/saved-crafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(savedCraftService);
        }

        @Test
        @WithTestUser
        @DisplayName("malformed JSON is a 400, not a 500")
        void malformedJsonIsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/saved-crafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"broken\",}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithTestUser
        @DisplayName("an unknown world surfaces as 400, not 500")
        void unknownWorldIsBadRequest() throws Exception {
            when(savedCraftService.createSavedCraftRequest(any(), any()))
                    .thenThrow(new UnknownWorldException("Unknown world: Faerei"));

            mockMvc.perform(post("/api/v1/saved-crafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithTestUser
        @DisplayName("a world that is not on the given data centre surfaces as 400")
        void mismatchedWorldIsBadRequest() throws Exception {
            when(savedCraftService.createSavedCraftRequest(any(), any()))
                    .thenThrow(new WorldDataCenterMismatchException("Faerie is on Aether, not Primal"));

            mockMvc.perform(post("/api/v1/saved-crafts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/saved-crafts/{id}")
    class Get {

        @Test
        @WithTestUser
        @DisplayName("passes user id and craft id in the right order")
        void argumentOrderIsCorrect() throws Exception {
            when(savedCraftService.getSavedCraft(any(), any())).thenReturn(sampleDto());

            mockMvc.perform(get("/api/v1/saved-crafts/{id}", CRAFT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CRAFT_ID.toString()));

            // Both parameters are UUID, so a swap compiles cleanly and silently 404s everything.
            // That exact bug shipped once already.
            verify(savedCraftService).getSavedCraft(USER_ID, CRAFT_ID);
        }

        @Test
        @WithTestUser
        @DisplayName("exposes priceScope so clients need not re-derive the null-world rule")
        void exposesPriceScope() throws Exception {
            when(savedCraftService.getSavedCraft(any(), any())).thenReturn(sampleDto());

            mockMvc.perform(get("/api/v1/saved-crafts/{id}", CRAFT_ID))
                    .andExpect(jsonPath("$.priceScope").value("Faerie"));
        }

        @Test
        @WithTestUser
        @DisplayName("someone else's craft is 404, never 403")
        void otherUsersCraftIsNotFound() throws Exception {
            when(savedCraftService.getSavedCraft(any(), any()))
                    .thenThrow(new SavedCraftNotFoundException("List not found"));

            // 403 would confirm the craft exists and allow enumeration by id.
            mockMvc.perform(get("/api/v1/saved-crafts/{id}", CRAFT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithTestUser
        @DisplayName("a malformed uuid in the path is a 400")
        void malformedIdIsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/saved-crafts/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithTestUser
        @DisplayName("listing a user's crafts passes their id and returns the summaries")
        void listReturnsSummaries() throws Exception {
            when(savedCraftService.getUserSavedCraft(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/saved-crafts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(savedCraftService).getUserSavedCraft(USER_ID);
        }
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    @Test
    @WithTestUser
    @DisplayName("DELETE returns 204 with no body")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/saved-crafts/{id}", CRAFT_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(savedCraftService).deleteSavedCraft(USER_ID, CRAFT_ID);
    }

    @Test
    @WithTestUser
    @DisplayName("an unsupported method on a valid path is 405, not 500")
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(put("/api/v1/saved-crafts/{id}", CRAFT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static CreateSavedCraftRequest validCreateRequest() {
        return CreateSavedCraftRequest.builder()
                .title("Gunblade run")
                .dataCenter("Aether")
                .world("Faerie")
                .notes("testing")
                .recipes(List.of(new SavedCraftRecipeRequest(RECIPE_ID, 1)))
                .build();
    }

    private static SavedCraftDto sampleDto() {
        return SavedCraftDto.builder()
                .id(CRAFT_ID)
                .title("Gunblade run")
                .dataCenter("Aether")
                .world("Faerie")
                .priceScope("Faerie")
                .notes("testing")
                .recipes(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Puts a real FfxivUserDetails into the security context.
     *
     * <p>@WithMockUser will not do here: the controller calls userDetails.getId(), and a mock
     * principal is a plain Spring User with no such method - every endpoint would fail on a
     * ClassCastException rather than on anything the test cares about.
     *
     * <p>The JWT itself is JwtAuthFilter's concern and is tested separately; minting real tokens
     * here would couple every controller test to the signing configuration.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = TestUserSecurityContextFactory.class)
    public @interface WithTestUser {
    }

    static class TestUserSecurityContextFactory implements WithSecurityContextFactory<WithTestUser> {

        @Override
        public SecurityContext createSecurityContext(WithTestUser annotation) {
            User user = User.builder()
                    .id(USER_ID)
                    .username("testuser")
                    .email("test@example.com")
                    .password("hashed")
                    .role(Role.USER)
                    .defaultDataCenter("Aether")
                    .defaultWorld("Faerie")
                    .build();

            FfxivUserDetails principal = new FfxivUserDetails(user);

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()));

            return securityContext;
        }
    }
}