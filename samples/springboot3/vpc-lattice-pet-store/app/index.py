import os
import json
import http.client

def handler(event, context):
  conn = http.client.HTTPSConnection(os.environ["ENDPOINT"])

  conn.request("GET", "/pets")
  res = conn.getresponse()
  data = res.read()

  return data