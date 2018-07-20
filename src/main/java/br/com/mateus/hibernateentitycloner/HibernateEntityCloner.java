package br.com.mateus.hibernateentitycloner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.ElementCollection;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;


/**
 * 
 * @author mateusparente
 * 
 */
public class HibernateEntityCloner {
	
	public static <T> T generateClone(Object source) {
		
		try {
			return populate(source);
		} catch (Exception e) {
			throw new HibernateEntityClonerException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T populate(Object source) throws Exception {
		
		Class<?> clazz = getClassForHibernateObject(source);
		
		T clonedModel = (T) clazz.newInstance();
			
		for(Field field : clazz.getDeclaredFields()){
			
			HibernateEntityClonerConfig configField = field.getAnnotation(HibernateEntityClonerConfig.class);
			
			if(configField != null && configField.ignore())
				continue;
			
			if(field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(PrimaryKeyJoinColumn.class)){
				if(configField == null || !configField.copyID()){
					continue;
				}
			}
			
			if(field.isAnnotationPresent(OneToMany.class)){
				
				List<T> itens = new ArrayList<>();
				
				field.setAccessible(true);
				List<?> persistentBag = (List<?>) getLazyObject(field, source);
				
				persistentBag.forEach(it -> {
					try { 
						
						T populatedItem = populate(it);
						
						String mappedByValue = field.getAnnotation(OneToMany.class).mappedBy();
						
						setBidirectionalMapping(clonedModel, populatedItem, mappedByValue);
						
						itens.add(populatedItem); 
					} catch (Exception e) {
						throw new HibernateEntityClonerException("Parse one-to-many error.");
					}
				});
				
				field.set(clonedModel, itens);
				
			} else if(field.isAnnotationPresent(OneToOne.class)) {
				
				field.setAccessible(true);
				
				Object attribute = populate(getLazyObject(field, source));
				
				String mappedByValue = field.getAnnotation(OneToOne.class).mappedBy();
				
				setBidirectionalMapping(clonedModel, attribute, mappedByValue);
				
				field.set(clonedModel, attribute);
				
			} else if(field.isAnnotationPresent(ManyToOne.class)){
				
				field.setAccessible(true);
				Object clonedValue = field.get(clonedModel);
				Object sourceValue = getLazyObject(field, source);
				
				if(clonedValue == null && sourceValue != null){
					
					Object attribute = field.getType().newInstance();
					
					List<Field> attIDs = Arrays.asList(field.getType().getDeclaredFields())
						.stream()
						.filter(attrField -> attrField.isAnnotationPresent(Id.class))
						.collect(Collectors.toList());
					
					attIDs.forEach(idField -> {
						try {
							idField.setAccessible(true);
							idField.set(attribute, idField.get(sourceValue));
							field.set(clonedModel, attribute);
						} catch (Exception e) {
							throw new HibernateEntityClonerException("Parse many-to-one error.");
						}
					});
				}
				
			} else if(field.isAnnotationPresent(ElementCollection.class) || field.isAnnotationPresent(ManyToMany.class)) {
				
				List<Object> itens = new ArrayList<>();
				field.setAccessible(true);
				
				List<?> persistentBag = (List<?>) getLazyObject(field, source);
				
				persistentBag.forEach(it -> {
					try {
						
						itens.add(it); 
						
					} catch (Exception e) {
						throw new HibernateEntityClonerException("Parse element collection error.");
					}
				});
				
				field.set(clonedModel, itens);
				
			} else {
				
				if(!Modifier.isStatic(field.getModifiers())){
					
					field.setAccessible(true);
					
					Object attribute = getLazyObject(field, source);
					field.set(clonedModel, attribute);
				}
			}
		}
		
		return clonedModel;
	}

	private static Object getLazyObject(Field field, Object object) throws IllegalArgumentException, IllegalAccessException{
		
		Object sourceValue = field.get(object);
		
		if (sourceValue instanceof HibernateProxy) {
	        LazyInitializer lazyInitializer = ((HibernateProxy) sourceValue).getHibernateLazyInitializer();
	        return lazyInitializer.getImplementation();
	    } else {
	    	return sourceValue;
	    }
	}
		
	private static <T> void setBidirectionalMapping(T clonedModel, Object attribute, String mappedByValue) throws IllegalAccessException {
		
		if(mappedByValue != null){
			try {
				Field field2 = attribute.getClass().getDeclaredField(mappedByValue);
				field2.setAccessible(true);
				field2.set(attribute, clonedModel);	
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static Class<?> getClassForHibernateObject(Object object) {
		if (object instanceof HibernateProxy) {
			LazyInitializer lazyInitializer = ((HibernateProxy) object).getHibernateLazyInitializer();
		    return lazyInitializer.getPersistentClass();
		} else {
			return object.getClass();
		}
	}
}
