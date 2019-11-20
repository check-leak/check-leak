#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <jvmti.h>

#include "com_dsect_jvmti_JVMTIInterface.h"

static jvmtiEnv *jvmti = NULL;
jvmtiEventCallbacks callbacks;

#define AGENT_MAIN

#include "agent.h"

jint initJVMTI(JavaVM *jvm)
{
   jint res;

   res = (*jvm)->GetEnv(jvm, (void **) &jvmti, JVMTI_VERSION_1_0);
   if (res!=JNI_OK) {
      return res;
   }

   jvmtiError error;


  jvmtiCapabilities   capabilities;

  error = (*jvmti)->GetCapabilities(jvmti, &capabilities);
  verifyError(jvmti, error);
  capabilities.can_tag_objects = 1;
  capabilities.can_generate_garbage_collection_events = 1;
  capabilities.can_generate_method_entry_events = 1;
  capabilities.can_generate_method_exit_events = 1;
  error= (*jvmti)->AddCapabilities(jvmti, &capabilities);
  verifyError(jvmti, error);

  return JNI_OK;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
   return initJVMTI(jvm);
}


//#define DEBUG 0

/** Local function for dealing with memory heaps... loading objects */
void memoryWriteReference(IterateControl* iterate, jlong tagReferrer, jlong tagReferee, jint index)
{
   fprintf (iterate->fileReferences,"%ld,%ld,%d\n",tagReferrer,tagReferee,index);
}

/** Local function for dealing with memory heaps... loading objects */
void memoryWriteThreadReference(IterateControl* iterate, jlong tagReferrer, jlong tagReferee, jint index)
{
   fprintf (iterate->fileReferences,"Thread%ld,%ld,%d\n",tagReferrer,tagReferee,index);
}

/** Local function for dealing with memory heaps... loading objects */
void memoryWriteObject(IterateControl* iterate, jlong objectTag, jlong classTag, jlong size)
{
   //long longclassTag = (long)classTag;
   fprintf (iterate->fileObjects,"%ld,%ld,%ld\n",objectTag,classTag,size);
}

/** Local function for dealing with memory heaps... loading classes */
void memoryWriteClass(JNIEnv *env,  jlong tag, IterateControl* iterate, jclass iterateClass)
{
    char * signature;
    char * genericPointer;

    jvmtiError err = (*jvmti)->GetClassSignature(jvmti, iterateClass,&signature,&genericPointer);
    if ( err != JVMTI_ERROR_NONE )
    {
        verifyError(jvmti, err);
        return;
    }


    jobject classLoader;
    (*jvmti)->GetClassLoader(jvmti, iterateClass,&classLoader);

    jlong tagLoader = 0;
    if (classLoader!=NULL)
    {
	    err = (*jvmti)->GetTag(jvmti, classLoader,&tagLoader);
	    if ( err != JVMTI_ERROR_NONE )
	    {
	        verifyError(jvmti, err);
	    }
	}

    if (tagLoader==0 && classLoader!=NULL)
    {
       tagLoader = iterate->genericCount++;
       (*jvmti)->SetTag(jvmti,classLoader,tagLoader);
       jlong classLoaderClassTag;
       jclass classLoaderClass = (*env)->GetObjectClass(env, classLoader);
       (*jvmti)->GetTag(jvmti, classLoaderClass,&classLoaderClassTag);
       jlong size;
       (*jvmti)->GetObjectSize(jvmti,classLoaderClass,&size);
       memoryWriteObject(iterate,tagLoader,classLoaderClassTag,size);
    }

    fprintf (iterate->fileClasses,"%ld,%s,%ld\n",tag,signature,tagLoader);



    if (signature!=NULL) (*jvmti)->Deallocate(jvmti, (unsigned char *)signature);
    if (genericPointer!=NULL) (*jvmti)->Deallocate(jvmti, (unsigned char *)genericPointer);
}

JNIEXPORT jobjectArray JNICALL Java_com_dsect_jvmti_JVMTIInterface_getLoadedClasses
  (JNIEnv * env, jobject thisObject)
{
  jclass loadedClass = (*env)->FindClass(env, "java/lang/Class");

#ifdef DEBUG
  fprintf (stderr,"LoadedClass = %ld\n",loadedClass);
#endif


  jint classCount=0;
  jclass * classesPointer;

  (*jvmti)->GetLoadedClasses(jvmti,&classCount,&classesPointer);

  // Set the object array
  jobjectArray arrayReturn = (*env)->NewObjectArray(env,classCount,loadedClass,0);

  for (jsize i=0;i<classCount;i++) {
     (*env)->SetObjectArrayElement(env,arrayReturn,i, classesPointer[i]);
  }

  (*jvmti)->Deallocate(jvmti,(unsigned char *)classesPointer);

  return arrayReturn;
}

/** Callback JVMTI function for threadReference */
static jvmtiIterationControl JNICALL iterateThreadReference
    (jvmtiHeapRootKind root_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     jlong thread_tag,
     jint depth,
     jmethodID method,
     jint slot,
     void* user_data)
{
     IterateControl * iterate = (IterateControl *) user_data;

     if ((*tag_ptr)==0)
     {
        *tag_ptr = iterate->genericCount++;
        memoryWriteObject(iterate, *tag_ptr,class_tag,size);
     }

     if (!class_tag)
     {
        fprintf (stderr,"ClassTAG can't be NULL... it happened at iterateThreadReference\n");
     }
#ifdef DEBUG
     fprintf (stderr,"Thread - classTag = %ld\n",class_tag);
#endif
     memoryWriteThreadReference(iterate, thread_tag, *tag_ptr, 0);
     return JVMTI_ITERATION_CONTINUE;
}

/** Callback JVMTI function for Root references */
jvmtiIterationControl JNICALL iterateRoot (jvmtiHeapRootKind root_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     void* user_data)
{

     IterateControl * iterate = (IterateControl *) user_data;
     if ((*tag_ptr)==0)
     {
        *tag_ptr = iterate->genericCount++;
        memoryWriteObject(iterate, *tag_ptr,class_tag,size);
     }

#ifdef DEBUG
     fprintf (stderr,"Root - classTag = %ld and class=%ld reference = %ld\n",class_tag,*tag_ptr);
#endif
     memoryWriteReference(iterate, 0, *tag_ptr, 0);
     return JVMTI_ITERATION_CONTINUE;
}

/** Callback JVMTI function for Object Relationships */
jvmtiIterationControl JNICALL iterateObjectRelationship
    (jvmtiObjectReferenceKind reference_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     jlong referrer_tag,
     jint referrer_index,
     void* user_data)
{
     /*if (reference_kind==JVMTI_REFERENCE_CLASS ||
         reference_kind==JVMTI_REFERENCE_CLASS_LOADER ||
         reference_kind==JVMTI_REFERENCE_INTERFACE ||
         reference_kind==JVMTI_REFERENCE_CONSTANT_POOL)
         {
                 return JVMTI_ITERATION_CONTINUE;
         } */

    IterateControl * iterate = (IterateControl *) user_data;

     if ((*tag_ptr)==0)
     {
        *tag_ptr = iterate->genericCount++;
        memoryWriteObject(iterate, *tag_ptr,class_tag,size);
     }

     memoryWriteReference(iterate, referrer_tag, *tag_ptr, referrer_index);

#ifdef DEBUG
//     fprintf (stderr,"Relationship between %ld and %ld\n",referrer_tag, *tag_ptr);
     fprintf (stderr,"Relationship between %ld ",*tag_ptr);
     fprintf (stderr,"and reftag=%ld\n",referrer_tag);
#endif
     return JVMTI_ITERATION_CONTINUE;
 }

jvmtiIterationControl JNICALL cleanTag
    (jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     void* user_data)
{
   *tag_ptr=0;
   return JVMTI_ITERATION_CONTINUE;
}


void releaseTags()
{
  (*jvmti)->IterateOverHeap( jvmti, JVMTI_HEAP_OBJECT_TAGGED,
				  &cleanTag, NULL);
}

JNIEXPORT void JNICALL Java_com_dsect_jvmti_JVMTIInterface_heapSnapshot
  (JNIEnv * env, jclass clazz, jstring classesFileName, jstring referencesFileName, jstring objectsFileName)
{

  jint classCount=0;
  jclass * classesPointer;

  const char * strClasses =   (*env)->GetStringUTFChars(env,classesFileName,NULL);
  const char * strReferences =   (*env)->GetStringUTFChars(env,referencesFileName,NULL);
  const char * strObjects =   (*env)->GetStringUTFChars(env,objectsFileName,NULL);


  IterateControl iterate;
  iterate.genericCount=0;
  iterate.fileObjects = fopen (strObjects,"w+");
  if (iterate.fileObjects==NULL) {
     fprintf (stderr,"couldn't open file %s\n",strObjects);
     throwException(env,"java/lang/RuntimeException","Couldn't open objects file");
     return;
  }
  fprintf (iterate.fileObjects,"objectTag,classTag,size\n");

  iterate.fileReferences = fopen (strReferences,"w+");
  if (iterate.fileReferences==NULL) {
     fprintf (stderr,"couldn't open file %s\n",strReferences);
     throwException(env,"java/lang/RuntimeException","Couldn't open references file");
     return;
  }
  fprintf (iterate.fileReferences,"tagReferrer,tagReferee,index\n");

  iterate.fileClasses = fopen (strClasses,"w+");
  if (iterate.fileClasses==NULL) {
     fprintf (stderr,"couldn't open file %s\n",strClasses);
     throwException(env,"java/lang/RuntimeException","Couldn't open classes file");
     return;
  }
  fprintf (iterate.fileClasses,"tagClass,signature,tagClassLoader\n");

  (*env)->ReleaseStringUTFChars(env, classesFileName,strClasses);
  (*env)->ReleaseStringUTFChars(env, referencesFileName,strReferences);
  (*env)->ReleaseStringUTFChars(env, objectsFileName,strObjects);

  (*jvmti)->GetLoadedClasses(jvmti, &classCount,&classesPointer);

  jvmtiError    err;

  // we need to first setAllTags, to avoid dependencies between classes and classLoaders
  for (jsize i=0;i<classCount;i++)
  {
     err = (*jvmti)->SetTag(jvmti, classesPointer[i],i+1);
    iterate.genericCount = i;
  }

  iterate.genericCount = iterate.genericCount + 1002;


  for (jsize i=0;i<classCount;i++)
  {
     jlong classTag;
     (*jvmti)->GetTag(jvmti, classesPointer[i],&classTag);
     //err = jvmti->SetTag(classesPointer[i],i);

     if ( err != JVMTI_ERROR_NONE )
     {
        verifyError(jvmti, err);
        return;
     }

     memoryWriteClass(env, classTag, &iterate,classesPointer[i]);

  }

  (*jvmti)->Deallocate(jvmti, (unsigned char *)classesPointer);

  err = (*jvmti)->IterateOverReachableObjects(jvmti, iterateRoot,
            iterateThreadReference,
            iterateObjectRelationship,
            &iterate);
  verifyError(jvmti, err);

  fclose(iterate.fileObjects);
  fclose(iterate.fileReferences);
  fclose(iterate.fileClasses);

  releaseTags();

  verifyError(jvmti, err);
}

JNIEXPORT void JNICALL Java_com_dsect_jvmti_JVMTIInterface_forceGC
  (JNIEnv * env, jobject thisObject)
{

   (*jvmti)->ForceGarbageCollection(jvmti);
}

jvmtiIterationControl JNICALL iterate_getAllObjects
    (jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     void* user_data)
{

    IteraOverObjectsControl * control = (IteraOverObjectsControl *) user_data;
    *tag_ptr=1;
    control->count++;

//    fprintf (stderr,"Iterate getAllObjects = %ld\n",control->count);

    return JVMTI_ITERATION_CONTINUE;
}


/*
 * Class:     com_dsect_jvmti_JVMTIInterface
 * Method:    getAllObjects
 * Signature: (Ljava/lang/Class;)[Ljava/lang/Object;
 */
JNIEXPORT jobjectArray JNICALL Java_com_dsect_jvmti_JVMTIInterface_getAllObjects
  (JNIEnv * env, jobject jvmtiInteface_this, jclass klass) {


  (*jvmti)->ForceGarbageCollection(jvmti);

  releaseTags();


  jclass loadedObject = (*env)->FindClass(env, "java/lang/Object");



  IteraOverObjectsControl control;
  control.size = 0;
  control.maxsize = 0;
  control.count=0;


  (*jvmti)->IterateOverInstancesOfClass(jvmti,
            klass,
            JVMTI_HEAP_OBJECT_EITHER,
            iterate_getAllObjects,
            &control);


  jint countObjts=0;
  jobject * objs;
  jlong * tagResults;

  jlong idToQuery=1;

  /// http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html#GetObjectsWithTags
  (*jvmti)->GetObjectsWithTags(jvmti, 1,
            &idToQuery,
            &countObjts,
            &objs,
            &tagResults);

  // Set the object array
  jobjectArray arrayReturn = (*env)->NewObjectArray(env,countObjts,loadedObject,0);

  for (jsize i=0;i<countObjts;i++) {
     (*env)->SetObjectArrayElement(env,arrayReturn,i, objs[i]);
  }

  (*jvmti)->Deallocate(jvmti,(unsigned char *)tagResults);
  (*jvmti)->Deallocate(jvmti,(unsigned char *)objs);

  releaseTags();


  return arrayReturn;
}


/** Callback JVMTI function for threadReference */
static jvmtiIterationControl JNICALL iterateThreadReferenceLookupReference
    (jvmtiHeapRootKind root_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     jlong thread_tag,
     jint depth,
     jmethodID method,
     jint slot,
     void* user_data)
{
	IteraOverObjectsControl * control = (IteraOverObjectsControl *)user_data;

	if (*tag_ptr==10)
	{
		addTag(jvmti,control,thread_tag);
	}
	else
	if (*tag_ptr!=10)
	{
		*tag_ptr=control->count++;
	}

    return JVMTI_ITERATION_CONTINUE;
}

/** Callback JVMTI function for Root references */
jvmtiIterationControl JNICALL iterateRootLookupReference (jvmtiHeapRootKind root_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     void* user_data)
{
	IteraOverObjectsControl * control = (IteraOverObjectsControl *)user_data;

	if (*tag_ptr!=10)
	{
		*tag_ptr=control->count++;
	}

     return JVMTI_ITERATION_CONTINUE;
}

/** Callback JVMTI function for Object Relationships */
jvmtiIterationControl JNICALL iterateObjectRelationshipLookupReference
    (jvmtiObjectReferenceKind reference_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     jlong referrer_tag,
     jint referrer_index,
     void* user_data)
{
	IteraOverObjectsControl * control = (IteraOverObjectsControl *)user_data;

	if (*tag_ptr==10)
	{
		addTag(jvmti,control,referrer_tag);
	}
	else
	if (*tag_ptr!=10)
	{
		*tag_ptr=control->count++;
	}

    return JVMTI_ITERATION_CONTINUE;
 }



JNIEXPORT jobjectArray JNICALL Java_com_dsect_jvmti_JVMTIInterface_getReferenceHolders
  (JNIEnv * env, jobject thisObject, jobjectArray objectArray)
{
	jobject referencedObject;


	jsize arrayLength = (*env)->GetArrayLength(env,objectArray);
	for (jsize i=0;i<arrayLength;i++)
	{
		referencedObject = (*env)->GetObjectArrayElement(env,objectArray,i);
		(*jvmti)->SetTag(jvmti, referencedObject,(jlong)10);
	}

    IteraOverObjectsControl control;
	control.size = 0;
	control.maxsize = 0;
	control.count=1000;
	control.tags=NULL;

    jvmtiError err = (*jvmti)->IterateOverReachableObjects(jvmti,iterateRootLookupReference,
            iterateThreadReferenceLookupReference,
            iterateObjectRelationshipLookupReference,
            &control);

    verifyError(jvmti, err);

    jint countObjts=0;
    jobject * objs=NULL;
    jlong * tagResults;

    /// http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html#GetObjectsWithTags
    (*jvmti)->GetObjectsWithTags(jvmti,control.size,
            control.tags,
            &countObjts,
            &objs,
            &tagResults);

    // Set the object array
    jclass loadedObject = (*env)->FindClass(env,"java/lang/Object");
    jobjectArray arrayReturn = (*env)->NewObjectArray(env,countObjts,loadedObject,0);

    for (jsize i=0;i<countObjts;i++) {
       (*env)->SetObjectArrayElement(env,arrayReturn,i, objs[i]);
    }

    if (control.tags!=NULL)
    {
    	(*jvmti)->Deallocate(jvmti,(unsigned char *)control.tags);
    }
    (*jvmti)->Deallocate(jvmti,(unsigned char *)tagResults);
    (*jvmti)->Deallocate(jvmti,(unsigned char *)objs);

	releaseTags();

	return arrayReturn;
}

JNIEXPORT void JNICALL Java_com_dsect_jvmti_JVMTIInterface_releaseTags
  (JNIEnv * env, jobject tag)
{
	releaseTags();
}

void writeReferenceOnNotify(FILE * fileReference,jlong referenceHolder,jlong referencedObject,jlong classTag,jlong index,jmethodID method,jbyte referenceType)
{
	if (fileReference!=NULL)
	{
		fwrite((const void *) &referenceHolder,sizeof(referenceHolder),1,fileReference);
		fwrite((const void *) &referencedObject,sizeof(referencedObject),1,fileReference);
		fwrite((const void *) &classTag,sizeof(classTag),1,fileReference);
		fwrite((const void *) &index,sizeof(index),1,fileReference);
		fwrite((const void *) &referenceType,sizeof(referenceType),1,fileReference);
		fwrite((const void *) &method,sizeof(method),1,fileReference);
	}
}

void writeObjectOnNotify(FILE * fileReference,jlong classTag,jlong objectTag,jlong bytes)
{
	if (fileReference!=NULL)
	{
		fwrite((const void *) &classTag,sizeof(classTag),1,fileReference);
		fwrite((const void *) &objectTag,sizeof(objectTag),1,fileReference);
		fwrite((const void *) &bytes,sizeof(bytes),1,fileReference);
	}
}

/** Callback JVMTI function for Root references used on notifyOnReferences*/
jvmtiIterationControl JNICALL iterateRootOnNotify (jvmtiHeapRootKind root_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     void* user_data)
{

     IterateControl * iterate = (IterateControl *) user_data;
     if ((*tag_ptr)==0)
     {
        *tag_ptr = iterate->genericCount++;

        writeObjectOnNotify(iterate->fileObjects, class_tag, *tag_ptr,size);
     }

     writeReferenceOnNotify(iterate->fileReferences,-1,*tag_ptr,0,0,NULL,10);

#ifdef DEBUG
     fprintf (stderr,"Root - classTag = %ld and class=%ld reference = %ld\n",class_tag,*tag_ptr);
#endif
     return JVMTI_ITERATION_CONTINUE;
}


/** Callback JVMTI function for threadReference used on notifyOnReferences*/
static jvmtiIterationControl JNICALL iterateThreadReferenceOnNotify
    (jvmtiHeapRootKind root_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     jlong thread_tag,
     jint depth,
     jmethodID method,
     jint slot,
     void* user_data)
{
     IterateControl * iterate = (IterateControl *) user_data;

     if ((*tag_ptr)==0)
     {
        *tag_ptr = iterate->genericCount++;
        writeObjectOnNotify(iterate->fileObjects, class_tag, *tag_ptr,size);
     }

     if (!class_tag)
     {
        fprintf (stderr,"ClassTAG can't be NULL... it happened at iterateThreadReference\n");
     }

#ifdef DEBUG
     fprintf (stderr,"Thread - classTag = %ld\n",class_tag);
#endif
     // meed to verify this option
     writeReferenceOnNotify(iterate->fileReferences,class_tag,*tag_ptr,class_tag,0,method,REFERENCE_THREAD);
     return JVMTI_ITERATION_CONTINUE;
}

/** Callback JVMTI function for Object Relationships used on notifyOnReferences*/
jvmtiIterationControl JNICALL iterateObjectRelationshipOnNotify
    (jvmtiObjectReferenceKind reference_kind,
     jlong class_tag,
     jlong size,
     jlong* tag_ptr,
     jlong referrer_tag,
     jint referrer_index,
     void* user_data)
{
     if (reference_kind==JVMTI_REFERENCE_CONSTANT_POOL)
         {
                 return JVMTI_ITERATION_CONTINUE;
         }
     /*if (reference_kind==JVMTI_REFERENCE_CLASS ||
         reference_kind==JVMTI_REFERENCE_CLASS_LOADER ||
         reference_kind==JVMTI_REFERENCE_INTERFACE ||
         reference_kind==JVMTI_REFERENCE_CONSTANT_POOL)
         {
                 return JVMTI_ITERATION_CONTINUE;
         } */

    IterateControl * iterate = (IterateControl *) user_data;
    if ((*tag_ptr)==0)
    {
       *tag_ptr = iterate->genericCount++;
       writeObjectOnNotify(iterate->fileObjects, class_tag, *tag_ptr,size);
    }

    writeReferenceOnNotify(iterate->fileReferences,
                           referrer_tag,
                           *tag_ptr,
                           class_tag,
                           referrer_index,
                           NULL,
                           reference_kind);
    return JVMTI_ITERATION_CONTINUE;
 }


JNIEXPORT void JNICALL Java_com_dsect_jvmti_JVMTIInterface_notifyInventory
  (JNIEnv *env, jobject thisObject, jboolean notifyClasses, jstring temporaryFileReferences, jstring temporaryFileObjects, jobject jvmtiCallBack)
{
  IterateControl iterate;
  releaseTags();

  jvmtiError err;
  if (temporaryFileReferences!=NULL)
  {
  	  const char * strTemporaryFile = strTemporaryFile = (*env)->GetStringUTFChars(env,temporaryFileReferences,NULL);
	  iterate.fileReferences = fopen(strTemporaryFile,"wb+");
	  if (iterate.fileReferences==NULL) {
             fprintf (stderr,"Couldn't open %s\n",strTemporaryFile);
	     throwException(env,"java/lang/RuntimeException","Couldn't open references file");
	     return;
	  }
      (*env)->ReleaseStringUTFChars(env,temporaryFileReferences,strTemporaryFile);
  }
  else
  {
  	iterate.fileReferences = NULL;
  }

  if (temporaryFileObjects!=NULL)
  {
  	  const char * strTemporaryFile = strTemporaryFile = (*env)->GetStringUTFChars(env,temporaryFileObjects,NULL);
	  iterate.fileObjects = fopen(strTemporaryFile,"wb+");
	  if (iterate.fileObjects==NULL) {
             fprintf (stderr,"Couldn't open %s\n",strTemporaryFile);
	     throwException(env,"java/lang/RuntimeException","Couldn't open objects file");
	     return;
	  }
      (*env)->ReleaseStringUTFChars(env,temporaryFileObjects,strTemporaryFile);
  }
  else
  {
  	iterate.fileObjects=NULL;
  }

  jint classCount=0;
  jclass * classesPointer;
  (*jvmti)->GetLoadedClasses(jvmti,&classCount,&classesPointer);
  // we need to first setAllTags, to avoid dependencies between classes and classLoaders
  for (jsize i=0;i<classCount;i++)
  {
     err = (*jvmti)->SetTag(jvmti,classesPointer[i],i+1);
  }

  jclass clazz = (*env)->GetObjectClass(env,jvmtiCallBack);

  jmethodID methodNotifyClass = (*env)->GetMethodID(env,clazz,"notifyClass","(JLjava/lang/Class;)V");
  if (methodNotifyClass==NULL)
  {
	     throwException(env,"java/lang/RuntimeException","Method notifyClass not found on JVMTICallBack");
	     return;
  }
  jmethodID notifyMethod = (*env)->GetMethodID(env,clazz,"notifyReference","(JJJJJB)V");
  if (notifyMethod==NULL)
  {
  	throwException(env,"java/lang/RuntimeException","Couldn't find notifyReference");
  	return ;
  }
  jmethodID notifyMethodObject = (*env)->GetMethodID(env,clazz,"notifyObject","(JJJ)V");
  if (notifyMethodObject==NULL)
  {
  	throwException(env,"java/lang/RuntimeException","Couldn't find notifyObject");
  	return ;
  }

  fflush(stderr);
  iterate.genericCount=classCount + 1000;
  (*jvmti)->ForceGarbageCollection(jvmti);


  fflush(stderr);
  err = (*jvmti)->IterateOverReachableObjects(jvmti,iterateRootOnNotify,
            iterateThreadReferenceOnNotify,
            iterateObjectRelationshipOnNotify,
            &iterate);
  verifyError(jvmti,err);

  if (notifyClasses)
  {
	  // we need to first setAllTags, to avoid dependencies between classes and classLoaders
	  for (jsize i=0;i<classCount;i++)
	  {
		    (*env)->CallVoidMethod(env,jvmtiCallBack,methodNotifyClass,(jlong)(i+1),classesPointer[i]);
	  }
  }

  (*jvmti)->Deallocate(jvmti,(unsigned char *)classesPointer);


  if (iterate.fileObjects!=NULL)
  {
	  fseek(iterate.fileObjects,SEEK_SET,0l);

	  jlong classTag=0;
	  jlong objectTag=0;
	  jlong bytes=0;
	  while (!feof(iterate.fileObjects))
	  {
         fread((void *) &classTag,sizeof(classTag),1,iterate.fileObjects);
         fread((void *) &objectTag,sizeof(objectTag),1,iterate.fileObjects);
         fread((void *) &bytes,sizeof(bytes),1,iterate.fileObjects);
	     (*env)->CallVoidMethod(env, jvmtiCallBack,notifyMethodObject,classTag,objectTag,bytes);
	  }

	  if (fclose(iterate.fileObjects))
          {
               fprintf (stderr,"Error on closing file on profiler\n");
          }

  }


  if (iterate.fileReferences!=NULL)
  {
	  fseek(iterate.fileReferences,SEEK_SET,0l);
	  jlong referenceHolder=0;
	  jlong referencedObject=0;
	  jlong classTag;
	  jlong index=0;
	  jbyte referenceType=0;
	  jmethodID method;
	  jlong methodParameter=0;

	  while (!feof(iterate.fileReferences))
	  {
		fread((void *) &referenceHolder,sizeof(referenceHolder),1,iterate.fileReferences);
		fread((void *) &referencedObject,sizeof(referencedObject),1,iterate.fileReferences);
		fread((void *) &classTag,sizeof(classTag),1,iterate.fileReferences);
		fread((void *) &index,sizeof(index),1,iterate.fileReferences);
		fread((void *) &referenceType,sizeof(referenceType),1,iterate.fileReferences);
		fread((void *) &method,sizeof(method),1,iterate.fileReferences);
		// this line generates a warning due to this convertion. You can safely ignore this
		methodParameter = (jlong)method;

	    (*env)->CallVoidMethod(env,jvmtiCallBack,notifyMethod,referenceHolder,referencedObject,classTag, index,methodParameter,referenceType);
	  }

	  if (fclose(iterate.fileReferences))
          {
               fprintf (stderr,"Error on closing file on profiler\n");
          }
  }

  verifyError(jvmti,err);
}

JNIEXPORT jlong JNICALL Java_com_dsect_jvmti_JVMTIInterface_getTagOnObject
  (JNIEnv * env, jobject thisObject, jobject taggedObject)
{
    jlong retTag;

    (*jvmti)->GetTag(jvmti,taggedObject,&retTag);

    return retTag;
}

JNIEXPORT jobject JNICALL Java_com_dsect_jvmti_JVMTIInterface_getObjectOnTag
  (JNIEnv * env, jobject thisObject, jlong tag)
{
    jint countObjts=0;
    jobject * objs;
    jlong * tagResults;

    if (tag<=0)
    {
		throwException(env,"java/lang/RuntimeException","Can't use a tag=0");
		return NULL;
    }

    /// http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html#GetObjectsWithTags
    (*jvmti)->GetObjectsWithTags(jvmti,1,
            &tag,
            &countObjts,
            &objs,
            &tagResults);

    jobject retObject = NULL;
    if (countObjts==1)
    {
    	retObject = objs[0];
    }

    (*jvmti)->Deallocate(jvmti,(unsigned char *)tagResults);
    (*jvmti)->Deallocate(jvmti,(unsigned char *)objs);

	return retObject;
}

JNIEXPORT jobject JNICALL Java_com_dsect_jvmti_JVMTIInterface_getObjectField
  (JNIEnv * env, jobject thisObject, jclass clazz, jboolean isStatic,jlong fieldIndex)
{
	jint fieldCount=0;
	jfieldID * fields;
	jvmtiError err = (*jvmti)->GetClassFields(jvmti,clazz,&fieldCount,&fields);
    if ( err != JVMTI_ERROR_NONE )
    {
        verifyError(jvmti, err);
        return NULL;
    }

    jobject field = NULL;


	if (fields!=NULL && fieldIndex<fieldCount)
	{
		//jfieldID fieldID = fields[fieldIndex];
	    field = (*env)->ToReflectedField(env,clazz,fields[fieldIndex],0);
	}

	if (fieldCount>0 && fields!=NULL)
	{
		(*jvmti)->Deallocate(jvmti,(unsigned char *)fields);
	}

	return field;

}


JNIEXPORT jstring JNICALL Java_com_dsect_jvmti_JVMTIInterface_getMethodName
  (JNIEnv * env, jobject thisObject, jlong lMethodId)
{
	if (lMethodId==0 || lMethodId==-1) return NULL;
	char * name;
	jmethodID id = (jmethodID)lMethodId;
	(*jvmti)->GetMethodName(jvmti,id,&name,NULL,NULL);

	jstring retString = (*env)->NewStringUTF(env, name);

	(*jvmti)->Deallocate(jvmti,(unsigned char *) name);

	return retString;
}

JNIEXPORT jstring JNICALL Java_com_dsect_jvmti_JVMTIInterface_getMethodSignature
  (JNIEnv * env, jobject thisObject, jlong lMethodId)
{
	if (lMethodId==0 || lMethodId==-1) return NULL;
	char * name;
	jmethodID id = (jmethodID)lMethodId;
	(*jvmti)->GetMethodName(jvmti,id,NULL,&name,NULL);

	jstring retString = (*env)->NewStringUTF(env,name);

	(*jvmti)->Deallocate(jvmti,(unsigned char *) name);

	return retString;
}


JNIEXPORT jclass JNICALL Java_com_dsect_jvmti_JVMTIInterface_getMethodClass
  (JNIEnv * env, jobject thisObject, jlong lMethodId)
{
	if (lMethodId==0 || lMethodId==-1) return NULL;
	jmethodID id = (jmethodID)lMethodId;
	jclass retClass;
	(*jvmti)->GetMethodDeclaringClass(jvmti,id,&retClass);

	return retClass;
}


JNIEXPORT jboolean JNICALL Java_com_dsect_jvmti_JVMTIInterface_internalIsConfiguredProperly
  (JNIEnv * env, jobject thisObject)
{
	return jvmti!=NULL;
}

JNICALL void eventMethodEntry(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method)
{
   fprintf (stderr,"Event captured method=%ld\n", method); fflush(stderr);
}


void JNICALL eventMethodLeave1(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method,
            jboolean was_popped_by_exception,
            jvalue return_value)
{
	fprintf (stderr,"Leave method=%ld\n", method);
}

JNICALL void eventMethodLeave2(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method)
{
   fprintf (stderr,"Enter method=%ld\n", method); fflush(stderr);
}


JNIEXPORT void JNICALL Java_com_dsect_jvmti_JVMTIInterface_startMeasure
  (JNIEnv * env, jobject thisObj, jstring jstrDirectory, jstring jstrPrefix, jstring jstrSuffix)
{
  fprintf (stderr,"Start measure\n"); fflush(stderr);

  //memset(&callbacks, 0, sizeof (callbacks));

  callbacks.MethodEntry = eventMethodEntry;
  callbacks.MethodExit = eventMethodLeave1;

  jvmtiError err = (*jvmti)->SetEventCallbacks(jvmti,&callbacks, sizeof (callbacks));
  if ( err != JVMTI_ERROR_NONE)
  {
      verifyError(jvmti,err);
      return;
  }

  err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, 0);
  if ( err != JVMTI_ERROR_NONE)
  {
      verifyError(jvmti, err);
      return;
  }

  err = (*jvmti)->SetEventNotificationMode(jvmti,JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, 0);
  if ( err != JVMTI_ERROR_NONE)
  {
      verifyError(jvmti, err);
      return;
  }

}


