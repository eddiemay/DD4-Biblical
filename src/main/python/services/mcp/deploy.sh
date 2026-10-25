#!/usr/bin/env bash
set -euo pipefail

# 🔧 Configuration (Hardcoded per your request)
PROJECT_ID="dd4-biblical"
SERVICE_NAME="mcp-server"
REGION="us-central1"
IMAGE_TAG="latest"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/cloud-run-source-deploy/${SERVICE_NAME}:${IMAGE_TAG}"

echo "🚀 Deploying ${SERVICE_NAME} to Cloud Run..."
echo "   Project: ${PROJECT_ID}"
echo "   Region:  ${REGION}"
echo "   Image:   ${IMAGE}"

# 2. Build & Push to Google Container Registry (Cloud Build handles PEP 621 automatically)
echo "📦 Building Docker image..."
gcloud builds submit --region "${REGION}" --project "${PROJECT_ID}" --tag "${IMAGE}" .

# 3. Deploy to Cloud Run (Managed)
echo "☁️  Deploying to Cloud Run..."
gcloud run deploy "${SERVICE_NAME}" --image "${IMAGE}" --region="${REGION}" --project="${PROJECT_ID}" --allow-unauthenticated

# 4. Output URL & verify
SERVICE_URL=$(gcloud run services describe "${SERVICE_NAME}" \
    --project "${PROJECT_ID}" \
    --platform managed \
    --region "${REGION}" \
    --format 'value(status.url)')

echo "✅ Deployment complete!"
echo "🔗 Service URL: ${SERVICE_URL}"

# Optional: Quick health check
echo "🌐 Testing endpoint..."
if curl -sf "${SERVICE_URL}" >/dev/null; then
    echo "✅ Service is responding."
else
    echo "⚠️ Service deployed but not yet responding. Check logs:"
    echo "   gcloud logging read 'resource.type=cloud_run_revision' --project=${PROJECT_ID}"
fi