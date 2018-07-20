# HibernateEntityCloner
Utility to create a clone of JPA entity. The result is a detached clone, ready to persist as new entity. 

Example of use:
--------------------------
ExampleEntity example = entityManager.find(ExampleEntity.class,1);

ExampleEntity exampleClone = HibernateEntityCloner.generateClone(example);
