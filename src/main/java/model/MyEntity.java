//package model;
//
//import java.util.List;
//
//import jakarta.inject.Inject;
//
//class EntityBase {
//	public static <E extends EntityBase> List<E> findAll() {
//		return null;
//	}
//}
//
//interface EntityRepo<E> {
//	public List<E> findAll();
//}
//
//public class MyEntity extends EntityBase {
//	public String text;
//	
//	static {
//		// this OK
//		List<MyEntity> entities = MyEntity.findAll();
//		// this not OK
//		for(MyEntity e : MyEntity.findAll()) {
//			
//		}
//		// this OK
//		MyEntity e1 = MyEntity.customFinder();
//	}
//	
//	// adding custom methods
//	public static MyEntity customFinder() {
//		return null;
//	}
//}
//
//class MyEntity3 {
////	@Inject
////	public static MyEntity2.MyEntityRepo REPO;
//}
//
//class MyEntity2 {
//	
//	public String text;
//	
//	static {
//		
//		// this OK
//		List<MyEntity2> entities = MyEntity2_.REPO.findAll();
//		// this also OK
//		for(MyEntity2 e : MyEntity2_.REPO.findAll()) {
//			
//		}
//		// this OK
//		MyEntity2 e1 = MyEntity2_.REPO.customFinder();
//	}
//	
////	@Inject
////	public static MyEntityRepo REPO;
//	
//	public interface MyEntityRepo extends EntityRepo<MyEntity2> {
//		// adding custom methods
//		public default MyEntity2 customFinder() {
//			return null;
//		}
//		public default MyEntity3 customFinderOther() {
//			return null;
//		}
//	}
//}
