{{/*
Expand the name of the chart.
*/}}
{{- define "sockshop.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "sockshop.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "sockshop.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "sockshop.labels" -}}
helm.sh/chart: {{ include "sockshop.chart" . }}
{{ include "sockshop.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "sockshop.selectorLabels" -}}
app.kubernetes.io/name: {{ include "sockshop.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the image path for a service
*/}}
{{- define "sockshop.image" -}}
{{- $registry := .global.imageRegistry }}
{{- $repository := .image.repository }}
{{- $tag := .image.tag }}
{{- printf "%s/%s:%s" $registry $repository $tag }}
{{- end }}

{{/*
Common labels for services
*/}}
{{- define "sockshop.service.labels" -}}
app: {{ .name }}
{{- end }}

{{/*
Selector labels for services
*/}}
{{- define "sockshop.service.selectorLabels" -}}
app: {{ .name }}
{{- end }}
