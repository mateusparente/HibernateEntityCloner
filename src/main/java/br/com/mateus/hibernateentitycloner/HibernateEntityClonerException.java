package br.com.mateus.hibernateentitycloner;

public class HibernateEntityClonerException extends RuntimeException  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8417287190451419280L;
	
	public HibernateEntityClonerException(String errorMessage) {
		super(errorMessage);
	}
	public HibernateEntityClonerException(Exception e) {
		super(e);
	}
}