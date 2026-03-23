package org.fipe.entrypoint.api1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fipe.application.InitialLoadService;
import org.fipe.application.VehicleQueryService;
import org.fipe.domain.BrandSummary;
import org.fipe.domain.VehicleDetails;
import org.fipe.entrypoint.api1.dto.InitialLoadResponse;
import org.fipe.entrypoint.api1.dto.ReferenceResponse;
import org.fipe.entrypoint.api1.dto.UpdateVehicleRequest;
import org.fipe.entrypoint.api1.dto.VehicleDetailsResponse;
import org.fipe.entrypoint.api1.dto.VehicleResponse;
import org.fipe.integration.fipe.FipeGateway;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@SecurityRequirement(name = "basicAuth")
public class FipeController {

    private final InitialLoadService initialLoadService;
    private final VehicleQueryService vehicleQueryService;
    private final FipeGateway fipeGateway;

    @GetMapping("/references")
    @Operation(summary = "Lista referencias FIPE", description = "Retorna as referencias mensais da FIPE v2.")
    public Flux<ReferenceResponse> getReferences() {
        return fipeGateway.fetchReferences()
                .map(reference -> new ReferenceResponse(reference.code(), reference.month()));
    }

    @PostMapping("/{vehicleType}/brands/initial-load")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Aciona a carga inicial", description = "Busca as marcas na FIPE v2 e envia cada marca para a fila.")
    @ApiResponse(responseCode = "202", description = "Carga inicial enviada para processamento")
    public Mono<InitialLoadResponse> triggerInitialLoad(
            @Parameter(schema = @Schema(allowableValues = {"cars", "motorcycles", "trucks"}))
            @PathVariable String vehicleType) {
        log.info("HTTP initial-load recebido: vehicleType={}", vehicleType);
        return initialLoadService.trigger(vehicleType)
                .map(context -> new InitialLoadResponse(
                        "Carga inicial enviada para a fila com sucesso.",
                        context.vehicleType(),
                        context.totalBrandsEnqueued()));
    }

    @GetMapping("/{vehicleType}/brands")
    @Operation(summary = "Lista marcas", description = "Retorna as marcas persistidas no formato compativel com a FIPE v2.")
    @ApiResponse(responseCode = "200", description = "Marcas encontradas",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BrandSummary.class))))
    public Flux<BrandSummary> getBrands(
            @Parameter(schema = @Schema(allowableValues = {"cars", "motorcycles", "trucks"}))
            @PathVariable String vehicleType) {
        return vehicleQueryService.findBrands(vehicleType);
    }

    @GetMapping("/{vehicleType}/brands/{brandId}/models")
    @Operation(summary = "Lista modelos por marca", description = "Retorna os modelos persistidos no formato compativel com a FIPE v2.")
    @ApiResponse(responseCode = "200", description = "Modelos encontrados",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleResponse.class))))
    public Flux<VehicleResponse> getVehiclesByBrand(
            @Parameter(schema = @Schema(allowableValues = {"cars", "motorcycles", "trucks"}))
            @PathVariable String vehicleType,
            @PathVariable String brandId) {
        return vehicleQueryService.findVehiclesByBrand(vehicleType, brandId)
                .map(FipeController::toResponse);
    }

    @GetMapping("/{vehicleType}/brands/{brandId}/models/{modelId}")
    @Operation(summary = "Busca detalhes internos do modelo", description = "Retorna o modelo salvo com observacoes internas.")
    public Mono<VehicleDetailsResponse> getVehicleDetails(
            @Parameter(schema = @Schema(allowableValues = {"cars", "motorcycles", "trucks"}))
            @PathVariable String vehicleType,
            @PathVariable String brandId,
            @PathVariable String modelId) {
        return vehicleQueryService.findVehicleById(vehicleType, brandId, modelId)
                .map(FipeController::toDetailsResponse);
    }

    @PutMapping("/{vehicleType}/brands/{brandId}/models/{modelId}")
    @Operation(summary = "Atualiza modelo salvo", description = "Permite alterar nome e observacoes do modelo armazenado.")
    public Mono<VehicleDetailsResponse> updateVehicle(
            @Parameter(schema = @Schema(allowableValues = {"cars", "motorcycles", "trucks"}))
            @PathVariable String vehicleType,
            @PathVariable String brandId,
            @PathVariable String modelId,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return vehicleQueryService.updateVehicle(
                        vehicleType,
                        brandId,
                        modelId,
                        request.modelName(),
                        request.observations())
                .map(FipeController::toDetailsResponse);
    }

    static VehicleResponse toResponse(VehicleDetails vehicleDetails) {
        return new VehicleResponse(vehicleDetails.vehicleCode(), vehicleDetails.modelName());
    }

    static VehicleDetailsResponse toDetailsResponse(VehicleDetails vehicleDetails) {
        return new VehicleDetailsResponse(
                vehicleDetails.vehicleType(),
                vehicleDetails.brandCode(),
                vehicleDetails.brandName(),
                vehicleDetails.vehicleCode(),
                vehicleDetails.modelName(),
                vehicleDetails.observations());
    }
}
