// It requires you to include "string.h" and "io.h"


#define REFERENCE_ROOT 10
#define REFERENCE_THREAD 11

typedef struct _iterateControl
{
   FILE * fileObjects;
   FILE * fileReferences;
   FILE * fileClasses;
   jlong genericCount;
} IterateControl;

typedef struct _iteraOverObjectsControl
{
   jint size;
   jint maxsize;
   jlong * tags;
   jlong count;
} IteraOverObjectsControl;



void verifyError(jvmtiEnv *jvmti, jvmtiError error) {
   if ( error != JVMTI_ERROR_NONE ) {
      char * errorName;
      (*jvmti)->GetErrorName(jvmti, error, &errorName);
      fprintf (stderr,"JVMTI Error %s\n",errorName);
      fflush(stderr);
      (*jvmti)->Deallocate(jvmti, (unsigned char *)errorName);
   }   
}

void addTag(jvmtiEnv *jvmti, IteraOverObjectsControl * control, jlong taglong)
{
   if (control->size>=control->maxsize)
   {
      unsigned char * buffer;
      jvmtiError error = (*jvmti)->Allocate(jvmti, sizeof(jlong) * (control->maxsize+1000),&buffer);
      verifyError(jvmti, error);
      jlong * newbuffer = (jlong *) buffer;
      
      if (control->tags!=NULL)
      {
	      for (jint i=0;i<control->size;i++) 
	      {
	         newbuffer[i] = control->tags[i];
	      }
	      (*jvmti)->Deallocate(jvmti, (unsigned char *)control->tags);
	  }
      control->tags = newbuffer;      
      control->maxsize=control->size+1000;
   }
   
   control->tags[control->size++] = taglong;
}

void throwException(JNIEnv * env,char * clazz, char * message);

jint initJVMTI(JavaVM *jvm);

inline void throwException(JNIEnv * env,char * clazz, char * message)
{
  jclass exceptionClass = (*env)->FindClass(env, clazz);
  if (exceptionClass==NULL) 
  {
     exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
     if (exceptionClass==NULL) 
     {
        fprintf (stderr,"Couldn't throw exception %s - %s\n",clazz,message);
     }
  }
  
  (*env)->ThrowNew(env, exceptionClass,message);
  
}
