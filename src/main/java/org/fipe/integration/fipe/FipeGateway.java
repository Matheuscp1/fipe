package org.fipe.integration.fipe;

import org.fipe.integration.fipe.dto.FipeBrand;
import org.fipe.integration.fipe.dto.FipeModel;
import org.fipe.integration.fipe.dto.FipeReference;
import reactor.core.publisher.Flux;

public interface FipeGateway {

    Flux<FipeReference> fetchReferences();

    Flux<FipeBrand> fetchBrands(String vehicleType, Integer reference);

    Flux<FipeModel> fetchModels(String vehicleType, String brandCode, Integer reference);
}
