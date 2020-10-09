#!/bin/bash

set -x

declare project=$GOOGLE_CLOUD_PROJECT
declare NUM=$(gcloud projects describe neo4j-to-go --format 'value(projectNumber)')
declare sa=$NUM-compute@developer.gserviceaccount.com

gcloud secrets add-iam-policy-binding neo4j-aura-uri \
  --member=serviceAccount:$sa \
  --role=roles/secretmanager.secretAccessor --project=$project &> /dev/null
  
gcloud secrets add-iam-policy-binding neo4j-aura-user \
  --member=serviceAccount:$sa \
  --role=roles/secretmanager.secretAccessor --project=$project &> /dev/null
  
gcloud secrets add-iam-policy-binding neo4j-aura-password \
  --member=serviceAccount:$sa \
  --role=roles/secretmanager.secretAccessor --project=$project &> /dev/null
