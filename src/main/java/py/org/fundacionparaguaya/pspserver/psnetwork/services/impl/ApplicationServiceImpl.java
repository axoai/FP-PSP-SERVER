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

import py.org.fundacionparaguaya.pspserver.psnetwork.entities.ApplicationEntity;
import py.org.fundacionparaguaya.pspserver.psnetwork.dtos.ApplicationEntityDTO;
import py.org.fundacionparaguaya.pspserver.psnetwork.repositories.ApplicationRepository;
import py.org.fundacionparaguaya.pspserver.psnetwork.services.ApplicationService;

@Service
public class ApplicationServiceImpl implements ApplicationService {

	private Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

	private ApplicationRepository applicationRepository;
	
	private ModelMapper modelMapper;
	 
	@Autowired
	public ApplicationServiceImpl(ApplicationRepository applicationRepository, ModelMapper modelMapper) {
		this.applicationRepository = applicationRepository;
		this.modelMapper = modelMapper;
	} 
	 

	@Override
	public ResponseEntity<ApplicationEntityDTO> addApplication(ApplicationEntityDTO applicationEntityDTO) {
		return new ResponseEntity<ApplicationEntityDTO>((ApplicationEntityDTO)
				convertToDto(applicationRepository.save((ApplicationEntity)
				convertToEntity(applicationEntityDTO, ApplicationEntity.class)), ApplicationEntityDTO.class), 
				HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<ApplicationEntityDTO> getApplicationById(Long applicationId) {
		ApplicationEntity application = applicationRepository.findOne(applicationId);
		if (application == null) {
			logger.debug("Application with id " , applicationId , " does not exists");
			return new ResponseEntity<ApplicationEntityDTO>(HttpStatus.NOT_FOUND);
		}
		logger.debug("Found Application: " , application);
		return new ResponseEntity<ApplicationEntityDTO>((ApplicationEntityDTO)convertToDto(application, ApplicationEntityDTO.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<List<ApplicationEntityDTO>> getAllApplications() {
		List<ApplicationEntity> applications = applicationRepository.findAll();
		if (applications.isEmpty()) {
			logger.debug("Applications does not exists");
			return new ResponseEntity<List<ApplicationEntityDTO>>(HttpStatus.NO_CONTENT);
		}
		logger.debug("Found " , applications.size() , " Applications");
		logger.debug(Arrays.toString(applications.toArray()));
		return new ResponseEntity<List<ApplicationEntityDTO>>(convertToDtoList(applications, List.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Void> deleteApplication(Long applicationId) {
		ApplicationEntity application = applicationRepository.findOne(applicationId);
		if (application == null) {
			logger.debug("Application with id " , applicationId , " does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		} else {
			applicationRepository.delete(applicationId);
			logger.debug("Application with id " , applicationId , " deleted");
			return new ResponseEntity<Void>(HttpStatus.GONE);
		}
	}
	
	public ResponseEntity<Void> updateApplication(ApplicationEntityDTO application){
		ApplicationEntity existingApplication = applicationRepository.findOne(application.getApplicationId());
		if (existingApplication == null) {
			logger.debug("Application with id " , application.getApplicationId() , " does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		} else {
			applicationRepository.save((ApplicationEntity)convertToEntity(application, ApplicationEntity.class));
			logger.debug("Updated:: " , application);
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
