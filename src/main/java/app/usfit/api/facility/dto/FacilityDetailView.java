// app/usfit/api/facility/dto/FacilityDetailView.java
package app.usfit.api.facility.dto;

public interface FacilityDetailView {
    Long getId();
    String getName();
    String getTypeName();
    String getSidoNm();
    String getSigunguNm();
    String getRoadAddr1();
    String getRoadAddr2();
    Double getLat();
    Double getLng();
    String getManagerPhone();
    Double getAreaSqm();
    String getIndoorOutdoor();
    Double getDistance();
}