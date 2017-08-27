package py.org.fundacionparaguaya.pspserver.psnetwork.services.impl;

import java.util.Arrays;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import py.org.fundacionparaguaya.pspserver.psnetwork.entities.OrganizationEntity;
import py.org.fundacionparaguaya.pspserver.psnetwork.dtos.OrganizationEntityDTO;
import py.org.fundacionparaguaya.pspserver.psnetwork.repositories.OrganizationRepository;
import py.org.fundacionparaguaya.pspserver.psnetwork.services.OrganizationService;

@Service
public class OrganizationServiceImpl implements OrganizationService {

	 private Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);

	 private OrganizationRepository organizationRepository;
	
	 private ModelMapper modelMapper;
	 
	 @Autowired
	 public OrganizationServiceImpl(OrganizationRepository organizationRepository, ModelMapper modelMapper) {
		this.organizationRepository = organizationRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public ResponseEntity<OrganizationEntityDTO> addOrganization(OrganizationEntityDTO organizationEntityDTO) {
		return new ResponseEntity<OrganizationEntityDTO>((OrganizationEntityDTO)
				convertToDto(organizationRepository.save((OrganizationEntity)
				convertToEntity(organizationEntityDTO, OrganizationEntity.class)), OrganizationEntityDTO.class), 
				HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<OrganizationEntityDTO> getOrganizationById(Long organizationId) {
		OrganizationEntity organization = organizationRepository.findOne(organizationId);
		if (organization == null) {
			logger.debug("Organization with id " + organizationId + " does not exists");
			return new ResponseEntity<OrganizationEntityDTO>(HttpStatus.NOT_FOUND);
		}
		logger.debug("Found Organization: " + organization);
		return new ResponseEntity<OrganizationEntityDTO>((OrganizationEntityDTO)convertToDto(organization, OrganizationEntityDTO.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<List<OrganizationEntityDTO>> getAllOrganizations() {
		List<OrganizationEntity> organizations = organizationRepository.findAll();
		if (organizations.isEmpty()) {
			logger.debug("Organizations does not exists");
			return new ResponseEntity<List<OrganizationEntityDTO>>(HttpStatus.NO_CONTENT);
		}
		logger.debug("Found " + organizations.size() + " Organizations");
		logger.debug(Arrays.toString(organizations.toArray()));
		return new ResponseEntity<List<OrganizationEntityDTO>>(convertToDtoList(organizations, List.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Void> deleteOrganization(Long organizationId) {
		OrganizationEntity organization = organizationRepository.findOne(organizationId);
		if (organization == null) {
			logger.debug("Organization with id " + organizationId + " does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		} else {
			organizationRepository.delete(organizationId);
			logger.debug("Organization with id " + organizationId + " deleted");
			return new ResponseEntity<Void>(HttpStatus.GONE);
		}
	}
	
	public ResponseEntity<Void> updateOrganization(OrganizationEntityDTO organization){
		OrganizationEntity existingOrganization = organizationRepository.findOne(organization.getOrganizationId());
		if (existingOrganization == null) {
			logger.debug("Organization with id " + organization.getOrganizationId() + " does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		} else {
			organizationRepository.save((OrganizationEntity)convertToEntity(organization, OrganizationEntity.class));
			logger.debug("Updated:: " + organization);
			return new ResponseEntity<Void>(HttpStatus.OK);
		}
		
	}

	@Override
	public List convertToDtoList(List list, Class c) {
		return (List) modelMapper.map(list, c);
	}


	@Override
	public Object convertToDto(Object entity, Class c) {
		 return modelMapper.map(entity, c);
	}


	@Override
	public Object convertToEntity(Object entity, Class c) {
		return  modelMapper.map(entity, c);
	}

}
