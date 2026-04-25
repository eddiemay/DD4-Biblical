import json
from datetime import datetime, timezone


class Agent:
  def __init__(self, llm, system:str=None, ip_address=None, creation_time=None, last_modified_time=None, messages=None):
    self.llm = llm
    self.system = system
    self.ip_address = ip_address
    self.creation_time = creation_time or datetime.now(timezone.utc)
    self.last_modified_time = last_modified_time
    self.messages = messages or []

  def to_dict(self):
    self.last_modified_time = datetime.now(timezone.utc)
    return {
      "system": self.system,
      "ipAddress": self.ip_address,
      "creationTime": self.creation_time,
      "lastModifiedTime": self.last_modified_time,
      "messages": json.dumps(self.messages)
    }

  @classmethod
  def from_dict(cls, llm, data):
    messages = data.get("messages")
    if type(messages) == list:
      messages = json.dumps(messages)
    return cls(
        llm,
        system = data.get("system"),
        ip_address = data.get("ipAddress"),
        creation_time = data.get("creationTime"),
        last_modified_time = data.get("lastModifiedTime"),
        messages = json.loads(messages) if messages else []
    )

  def trim_messages(self, max_messages=20):
    # Then trim to size.
    if len(self.messages) > max_messages:
      self.messages = self.messages[-(max_messages):]

  def execute(self, message):
    messages = []
    if self.system:
      messages.append({"role": "system", "content": self.system})
    messages.extend(self.messages)
    messages.append(message)
    return self.llm.invoke(messages).content