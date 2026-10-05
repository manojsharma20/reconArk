{{- define "reconark.labels" -}}
app.kubernetes.io/name: {{ .Values.name }}
app.kubernetes.io/part-of: reconark
app.kubernetes.io/version: {{ .Values.image.tag | quote }}
{{- end -}}
