package py.org.fundacionparaguaya.pspserver.reports.services.impl;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import py.org.fundacionparaguaya.pspserver.common.utils.StringConverter;
import py.org.fundacionparaguaya.pspserver.families.entities.FamilyEntity;
import py.org.fundacionparaguaya.pspserver.families.repositories.FamilyRepository;
import py.org.fundacionparaguaya.pspserver.families.specifications.FamilySpecification;
import py.org.fundacionparaguaya.pspserver.network.entities.OrganizationEntity;
import py.org.fundacionparaguaya.pspserver.reports.dtos.FamilySnapshotDTO;
import py.org.fundacionparaguaya.pspserver.reports.dtos.OrganizationFamilyDTO;
import py.org.fundacionparaguaya.pspserver.reports.dtos.ReportDTO;
import py.org.fundacionparaguaya.pspserver.reports.dtos.SnapshotFilterDTO;
import py.org.fundacionparaguaya.pspserver.reports.mapper.FamilyDTOMapper;
import py.org.fundacionparaguaya.pspserver.reports.services.SnapshotReportManager;
import py.org.fundacionparaguaya.pspserver.surveys.dtos.SurveyData;
import py.org.fundacionparaguaya.pspserver.surveys.entities.SnapshotEconomicEntity;
import py.org.fundacionparaguaya.pspserver.surveys.entities.SurveyEntity;
import py.org.fundacionparaguaya.pspserver.surveys.enums.SurveyStoplightEnum;
import py.org.fundacionparaguaya.pspserver.surveys.mapper.SnapshotIndicatorMapper;
import py.org.fundacionparaguaya.pspserver.surveys.repositories.SnapshotEconomicRepository;
import py.org.fundacionparaguaya.pspserver.surveys.repositories.SurveyRepository;
import py.org.fundacionparaguaya.pspserver.surveys.specifications.SnapshotEconomicSpecification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.Specifications.where;
import static py.org.fundacionparaguaya.pspserver.families.specifications.FamilySpecification.byApplication;
import static py.org.fundacionparaguaya.pspserver.families.specifications.FamilySpecification.byOrganization;
import static py.org.fundacionparaguaya.pspserver.surveys.specifications.SnapshotEconomicSpecification.forFamily;

/**
 *
 * @author mgonzalez
 *
 */
@Service
public class SnapshotReportManagerImpl implements SnapshotReportManager {

    private static final List<String> DEFAULT_HEADERS = Arrays.asList(
            "Organization Name", "Family Code", "Family Name", "Created At");

    private static final String CSV_DELIMITER = ",";

    private final FamilyRepository familyRepository;

    private final FamilyDTOMapper familyReportMapper;

    private final SnapshotEconomicRepository snapshotRepository;

    private final SnapshotIndicatorMapper snapshotMapper;

    private final SurveyRepository surveyRepository;

    public SnapshotReportManagerImpl(FamilyRepository familyRepository,
                                     FamilyDTOMapper familyReportMapper,
                                     SnapshotEconomicRepository snapshotRepository,
                                     SnapshotIndicatorMapper snapshotMapper,
                                     SurveyRepository surveyRepository) {
        this.familyRepository = familyRepository;
        this.familyReportMapper = familyReportMapper;
        this.snapshotRepository = snapshotRepository;
        this.snapshotMapper = snapshotMapper;
        this.surveyRepository = surveyRepository;
    }

    @Override
    public List<OrganizationFamilyDTO> listFamilyByOrganizationAndCreatedDate(
            SnapshotFilterDTO filters) {

        List<FamilyEntity> families = new ArrayList<>();

        Sort sort = new Sort(new Sort.Order(Direction.ASC, "organization.name"),
                new Sort.Order(Direction.ASC, "name"));

        Specification<FamilyEntity> dateRange = FamilySpecification
                .createdAtBetween2Dates(filters.getDateFrom(),
                        filters.getDateTo());

        families = familyRepository.findAll(where(byOrganization(filters.getOrganizationId()))
              .and(dateRange)
              .and(byApplication(filters.getApplicationId()))
              .and(dateRange), sort);

        Map<OrganizationEntity, List<FamilyEntity>> groupByOrganization = families
                .stream()
                .filter(f -> f != null && f.getOrganization() != null )
                .collect(Collectors.groupingBy(f -> f.getOrganization()));

        List<OrganizationFamilyDTO> toRet = new ArrayList<>();

        groupByOrganization.forEach((k, v) -> {
            OrganizationFamilyDTO fa = new OrganizationFamilyDTO(k.getName(),
                    k.getCode(), k.getDescription(), k.isActive());
            fa.setFamilies(familyReportMapper.entityListToDtoList(v));

            toRet.add(fa);

        });

        return toRet;

    }

    @Override
    public List<FamilySnapshotDTO> listSnapshotByFamily(
            SnapshotFilterDTO filters) {
        List<FamilySnapshotDTO> toRet = new ArrayList<>();

        Sort sort = new Sort(new Sort.Order(Direction.ASC, "createdAt"));

        if (filters.getDateFrom() != null && filters.getDateTo() != null
                && filters.getFamilyId() != null) {

            List<SnapshotEconomicEntity> snapshots = snapshotRepository.findAll(
                    where(forFamily(filters.getFamilyId()))
                            .and(SnapshotEconomicSpecification
                                    .createdAtBetween2Dates(
                                            filters.getDateFrom(),
                                            filters.getDateTo())),
                    sort);

            Map<SurveyEntity, List<SnapshotEconomicEntity>> groupBySurvey = snapshots
                    .stream().collect(Collectors
                            .groupingBy(s -> s.getSurveyDefinition()));

            groupBySurvey.forEach((k, v) -> {

                FamilySnapshotDTO familySnapshots = new FamilySnapshotDTO(
                        filters.getFamilyId(), k.getTitle());
                familySnapshots.setSnapshots(getSnasphots(v));
                toRet.add(familySnapshots);

            });

        }

        return toRet;
    }

    private ReportDTO getSnasphots(List<SnapshotEconomicEntity> snapshots) {
        ReportDTO report = new ReportDTO();

        report.getHeaders().add("Created At");

        List<SurveyData> rows = new ArrayList<>();

        report.getHeaders().addAll(snapshotMapper.getStaticPropertiesNames());

        for (SnapshotEconomicEntity s : snapshots) {

            s.getSnapshotIndicator().getAdditionalProperties()
                    .forEach((k, v) -> {
                        if (!report.getHeaders().contains(
                                StringConverter.getNameFromCamelCase(k))) {
                            report.getHeaders().add(
                                    StringConverter.getNameFromCamelCase(k));
                        }
                    });
            SurveyData data = snapshotMapper
                    .entityToDto(s.getSnapshotIndicator());
            data.put("createdAt", s.getCreatedAtLocalDateString());
            rows.add(data);
        }

        report.setRows(generateRows(rows, report.getHeaders()));
        return report;

    }

    private ReportDTO getOrganizationAndFamilyData(
            List<SnapshotEconomicEntity> snapshots) {

        ReportDTO report = new ReportDTO();

        report.getHeaders().addAll(DEFAULT_HEADERS);

        List<SurveyData> rows = new ArrayList<>();

        report.getHeaders().addAll(snapshotMapper.getStaticPropertiesNames());

        for (SnapshotEconomicEntity s : snapshots) {

            s.getSnapshotIndicator().getAdditionalProperties()
                    .forEach((k, v) -> {
                        String headerName = StringConverter.getNameFromCamelCase(k);
                        if (!report.getHeaders().contains(headerName)) {
                            report.getHeaders().add(headerName);
                        }
                    });
            SurveyData data = snapshotMapper
                    .entityToDto(s.getSnapshotIndicator());
            data.put("organizationName",
                    s.getFamily().getOrganization().getName());
            data.put("familyCode", s.getFamily().getCode());
            data.put("familyName", s.getFamily().getName());
            data.put("snapshotCreatedAt", s.getCreatedAtLocalDateString());
            rows.add(data);
        }

        report.setRows(generateRows(rows, report.getHeaders()));
        return report;

    }

    private List<List<String>> generateRows(List<SurveyData> rowsValue,
            List<String> headers) {

        List<List<String>> rows = new ArrayList<>();

        for (SurveyData data : rowsValue) {

            List<String> row = new ArrayList<>();

            for (String header : headers) {

                String key = StringConverter.getCamelCaseFromName(header);

                if (data.containsKey(key)) {
                    if (data.getAsString(key) == null) {
                        row.add("");
                    } else {
                        // row.add(getIndicatorValues(data.getAsString(key)));
                        row.add(getIndicatorValues(data.getAsString(key)));
                    }
                } else {
                    row.add("");
                }
            }
            rows.add(row);
        }

        return rows;
    }

    @Override
    public String generateCSVSnapshotByOrganizationAndCreatedDate(SnapshotFilterDTO filters) {
        ReportDTO report = getSnapshotsReportByOrganizationAndCreatedDate(filters);
        return reportToCsv(report);
    }

    @Override
    public ReportDTO getSnapshotsReportByOrganizationAndCreatedDate(SnapshotFilterDTO filters) {
        List<SnapshotEconomicEntity> snapshots = new ArrayList<>();

        Sort sort = new Sort(
                new Sort.Order(Direction.ASC, "family.organization.name"),
                new Sort.Order(Direction.ASC, "family.name"),
                new Sort.Order(Direction.ASC, "createdAt"));

        if (filters.getDateFrom() != null && filters.getDateTo() != null) {
            Specification<SnapshotEconomicEntity> dateRange = SnapshotEconomicSpecification
                    .createdAtBetween2Dates(filters.getDateFrom(),
                            filters.getDateTo());

            snapshots = snapshotRepository.findAll(
                  where(SnapshotEconomicSpecification
                          .byApplication(filters.getApplicationId()))
                                  .and(dateRange)
                                  .and(SnapshotEconomicSpecification
                                          .byOrganizations(filters.getOrganizationId())), sort);
        }

        ReportDTO report = getOrganizationAndFamilyData(snapshots);
        return report;
    }

    @Override
    public String downloadSnapshotsCSV(SnapshotFilterDTO filters) {
        ReportDTO report = getSnapshotsReport(filters);
        return reportToCsv(report);
    }

    @Override
    public ReportDTO getSnapshotsReport(SnapshotFilterDTO filters) {
        List<SnapshotEconomicEntity> snapshots = new ArrayList<>();

        Sort sort = new Sort(
                new Sort.Order(Direction.ASC, "family.organization.name"),
                new Sort.Order(Direction.ASC, "family.name"),
                new Sort.Order(Direction.ASC, "createdAt"));

        if (filters.getDateFrom() != null && filters.getDateTo() != null) {
            Specification<SnapshotEconomicEntity> dateRange = SnapshotEconomicSpecification
                    .createdAtBetween2Dates(filters.getDateFrom(),
                            filters.getDateTo());

            snapshots = snapshotRepository.findAll(
                    where(SnapshotEconomicSpecification.forSurvey(filters.getSurveyId()))
                            .and(SnapshotEconomicSpecification.byApplication(filters.getApplicationId()))
                            .and(dateRange)
                            .and(SnapshotEconomicSpecification.byOrganizations(filters.getOrganizationId())), sort);
        }

        ReportDTO report = new ReportDTO();

        report.getHeaders().addAll(DEFAULT_HEADERS);

        List<SurveyData> rows = new ArrayList<>();

        SurveyEntity survey = surveyRepository.findById(filters.getSurveyId());

        List<String> personalInformationKeys = survey.getSurveyDefinition().getSurveyUISchema().getGroupPersonal();
        // Ordenando aca personalInformationKeys como en ui:order hará que aparaescan ordenados en el CSV
        for (String key : personalInformationKeys) {
            String headerName = StringConverter.getNameFromCamelCase(key);
            report.getHeaders().add(headerName);
        }

        List<String> socioEconomicsKeys = survey.getSurveyDefinition().getSurveyUISchema().getGroupEconomics();
        // Ordenando aca socioEconomicsKeys como en ui:order hará que aparaescan ordenados en el CSV
        for (String key : socioEconomicsKeys) {
            String headerName = StringConverter.getNameFromCamelCase(key);
            report.getHeaders().add(headerName);
        }

        List<String> indicatorsKeys = survey.getSurveyDefinition().getSurveyUISchema().getGroupIndicators();
        // Ordenando aca indicatorsKeys como en ui:order hará que aparaescan ordenados en el CSV
        for (String key : indicatorsKeys) {
            String headerName = StringConverter.getNameFromCamelCase(key);
            report.getHeaders().add(headerName);
        }

        for (SnapshotEconomicEntity s : snapshots) {

            SurveyData data = snapshotMapper.entityToDto(s.getSnapshotIndicator());

            data.put("organizationName", s.getFamily().getOrganization().getName());
            data.put("familyCode", s.getFamily().getCode());
            data.put("familyName", s.getFamily().getName());
            data.put("createdAt", s.getCreatedAtLocalDateString());

            // Agregar aca todos los personal information que aparecen en personalInformationKeys, desde el entity Person

            // Agregar aca todos los socio-economics information que aparecen en socioEconomicsKeys, desde el entity SnapshotEconomicEntity

            rows.add(data);
        }

        report.setRows(generateRows(rows, report.getHeaders()));

        return report;
    }

    private String reportToCsv(ReportDTO report) {

        String toRet = report.getHeaders().stream().map(Object::toString)
                .collect(Collectors.joining(CSV_DELIMITER)).concat("\n");

        for (List<String> row : report.getRows()) {
            toRet = toRet + (row.stream().map(Object::toString)
                    .collect(Collectors.joining(CSV_DELIMITER))).concat("\n");
        }

        return toRet;
    }

    private String getIndicatorValues(String value) {

        SurveyStoplightEnum surveyStoplightEnum = SurveyStoplightEnum
                .fromValue(value);
        if (surveyStoplightEnum != null) {
            return String.valueOf(surveyStoplightEnum.getCode());
        }

        return value;

    }
}
