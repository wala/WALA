package annotations;

@RuntimeInvisableAnnotation
@RuntimeVisableAnnotation
@DefaultVisableAnnotation
public class AnnotatedClass1 {

  @RuntimeVisableAnnotationForMethod
  @RuntimeInvisableAnnotationForMethod
  public void m1(){
    
  }
  
}
