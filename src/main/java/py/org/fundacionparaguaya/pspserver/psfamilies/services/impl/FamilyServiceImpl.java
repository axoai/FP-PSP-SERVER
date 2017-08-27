package py.org.fundacionparaguaya.pspserver.psfamilies.services.impl;

import java.util.Arrays;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import py.org.fundacionparaguaya.pspserver.psfamilies.entities.FamilyEntity;
import py.org.fundacionparaguaya.pspserver.psfamilies.dtos.FamilyEntityDTO;
import py.org.fundacionparaguaya.pspserver.psfamilies.repositories.FamilyRepository;
import py.org.fundacionparaguaya.pspserver.psfamilies.services.FamilyService;

@Service
public class FamilyServiceImpl implements FamilyService {

	private Logger logger = LoggerFactory.getLogger(FamilyServiceImpl.class);

	private FamilyRepository familyRepository;
	
	private ModelMapper modelMapper;
	
	@Autowired
	public FamilyServiceImpl(FamilyRepository familyRepository, ModelMapper modelMapper) {
		this.familyRepository = familyRepository;
		this.modelMapper = modelMapper;
	}
	 

	@Override
	public ResponseEntity<FamilyEntityDTO> addFamily(FamilyEntityDTO familyEntityDTO) {
		return new ResponseEntity<FamilyEntityDTO>((FamilyEntityDTO)
				convertToDto(familyRepository.save((FamilyEntity)
				convertToEntity(familyEntityDTO, FamilyEntity.class)), FamilyEntityDTO.class), 
				HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<FamilyEntityDTO> getFamilyById(Long familyId) {
		FamilyEntity family = familyRepository.findOne(familyId);
		if (family == null) {
			logger.debug("Family with id " + familyId + " does not exists");
			return new ResponseEntity<FamilyEntityDTO>(HttpStatus.NOT_FOUND);
		}
		logger.debug("Found Family: " + family);
		return new ResponseEntity<FamilyEntityDTO>((FamilyEntityDTO)convertToDto(family, FamilyEntityDTO.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<List<FamilyEntityDTO>> getAllFamilies() {
		List<FamilyEntity> families = familyRepository.findAll();
		if (families.isEmpty()) {
			logger.debug("Families does not exists");
			return new ResponseEntity<List<FamilyEntityDTO>>(HttpStatus.NO_CONTENT);
		}
		logger.debug("Found " + families.size() + " Families");
		logger.debug("Found " + families);
		logger.debug(Arrays.toString(families.toArray()));
		return new ResponseEntity<List<FamilyEntityDTO>>(convertToDtoList(families, List.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Void> deleteFamily(Long familyId) {
		FamilyEntity family = familyRepository.findOne(familyId);
		if (family == null) {
			logger.debug("Family with id " + familyId + " does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		} else {
			familyRepository.delete(familyId);
			logger.debug("Family with id " + familyId + " deleted");
			return new ResponseEntity<Void>(HttpStatus.OK);
		}
	}
	
	public ResponseEntity<Void> updateFamily(FamilyEntityDTO family){
		FamilyEntity existingFamily = familyRepository.findOne(family.getFamilyId());
		if (existingFamily == null) {
			logger.debug("Family with id " + family.getFamilyId() + " does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		} else {
			familyRepository.save((FamilyEntity)convertToEntity(family, FamilyEntity.class));
			logger.debug("Updated:: " + family);
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
