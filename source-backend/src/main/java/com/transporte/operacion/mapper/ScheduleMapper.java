package com.transporte.operacion.mapper;

import com.transporte.operacion.dto.ScheduleRequest;
import com.transporte.operacion.dto.ScheduleResponse;
import com.transporte.operacion.entity.Schedule;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "bus", ignore = true)
    @Mapping(target = "driver", ignore = true)
    Schedule toEntity(ScheduleRequest request);

    @Mapping(target = "routeId", source = "route.id")
    @Mapping(target = "routeDescription", source = "route.description")
    @Mapping(target = "busId", source = "bus.id")
    @Mapping(target = "busPlate", source = "bus.plate")
    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverName", expression = "java(schedule.getDriver() != null ? schedule.getDriver().getFirstName() + \" \" + schedule.getDriver().getLastName() : null)")
    ScheduleResponse toResponse(Schedule schedule);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "bus", ignore = true)
    @Mapping(target = "driver", ignore = true)
    void updateFromRequest(ScheduleRequest request, @MappingTarget Schedule schedule);
}
