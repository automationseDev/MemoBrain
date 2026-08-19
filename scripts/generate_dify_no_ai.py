"""Generate the v0.3.8 Dify DSL with deterministic native-app actions."""

from copy import deepcopy
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "dify" / "MemoBrain_DifyOnly_v0.3.7.yml"
TARGET = ROOT / "dify" / "MemoBrain_DifyOnly_v0.3.8.yml"


def edge(template, source, handle, target, source_type, target_type):
    value = deepcopy(template)
    value["id"] = f"{source}-{handle}-{target}"
    value["source"] = source
    value["sourceHandle"] = handle
    value["target"] = target
    value["data"]["sourceType"] = source_type
    value["data"]["targetType"] = target_type
    return value


def node(template, node_id, title, x, y):
    value = deepcopy(template)
    value["id"] = node_id
    value["data"]["title"] = title
    value["position"] = {"x": x, "y": y}
    value["positionAbsolute"] = {"x": x, "y": y}
    return value


def code_node(template, node_id, title, code, variables, outputs, x, y):
    value = node(template, node_id, title, x, y)
    value["data"]["code"] = code
    value["data"]["variables"] = [
        {"value_selector": selector, "variable": name} for name, selector in variables
    ]
    value["data"]["outputs"] = {
        name: {"children": None, "type": kind} for name, kind in outputs.items()
    }
    return value


def answer_node(template, node_id, title, answer, x, y):
    value = node(template, node_id, title, x, y)
    value["data"]["answer"] = answer
    return value


def main():
    document = yaml.safe_load(SOURCE.read_text(encoding="utf-8"))
    document["app"]["name"] = "MemoBrain - Agent v0.3.8"
    document["app"]["description"] = (
        "MemoBrain v0.3.8: Androidの構造化actionによる一覧・詳細・検索・絞り込み・"
        "TODO完了・読了をLLMなしで処理。従来の保存・質問・Web調査フローも維持。"
    )
    graph = document["workflow"]["graph"]
    nodes = graph["nodes"]
    edges = graph["edges"]
    by_id = {item["id"]: item for item in nodes}

    by_id["start"]["data"]["variables"] = [
        {"label": label, "max_length": 256, "options": [], "required": False,
         "type": "text-input", "variable": variable}
        for label, variable in [
            ("MemoBrain action", "action"), ("検索語または対象", "query"),
            ("カテゴリ", "category"), ("タグ", "tag")
        ]
    ]

    router_template = deepcopy(by_id["action_router"])
    code_template = deepcopy(by_id["search_build"])
    http_post_template = deepcopy(by_id["search_http"])
    http_get_template = deepcopy(by_id["list_http"])
    answer_template = deepcopy(by_id["list_answer"])
    edge_template = deepcopy(edges[0])

    entry_router = node(router_template, "native_entry_router", "非AI action判定", 600, 300)
    entry_router["data"]["desc"] = "actionがあるAndroidリクエストはLLM解析を迂回"
    entry_router["data"]["cases"] = [{
        "case_id": "native", "id": "native", "logical_operator": "and",
        "conditions": [{"comparison_operator": "not empty", "id": "native-action-present",
                        "value": "", "varType": "string",
                        "variable_selector": ["start", "action"]}]
    }]

    actions = ["knowledge_list", "knowledge_search", "knowledge_detail", "todo_list",
               "read_later_list", "todo_complete", "read_later_complete"]
    native_router = node(router_template, "native_action_router", "非AI操作ルーター", 5750, 1120)
    native_router["data"]["desc"] = "Androidが明示したactionだけを機械的に分岐"
    native_router["data"]["cases"] = [
        {"case_id": action, "id": action, "logical_operator": "and",
         "conditions": [{"comparison_operator": "is", "id": f"native-{action}",
                         "value": action, "varType": "string",
                         "variable_selector": ["start", "action"]}]}
        for action in actions
    ]

    direct_list_http = node(http_get_template, "native_list_http", "非AI ナレッジ一覧取得", 6100, 1020)
    direct_list_http["data"]["url"] = "{{#env.DIFY_API_BASE#}}/datasets/{{#conversation.dataset_id#}}/documents?page=1&limit=50"
    direct_list_format = code_node(
        code_template, "native_list_format", "非AI 一覧JSON整形",
        '''import json\nfrom datetime import datetime,timezone,timedelta\ndef main(body:str)->dict:\n    try: data=json.loads(body or "{}")\n    except: data={}\n    items=[]\n    for row in (data.get("data") or [])[:50]:\n        stamp=row.get("created_at"); created=""\n        if isinstance(stamp,(int,float)):\n            created=datetime.fromtimestamp(stamp,timezone.utc).astimezone(timezone(timedelta(hours=9))).strftime("%Y-%m-%d %H:%M")\n        items.append({"title":str(row.get("name") or "(無題)"),"document_id":str(row.get("id") or ""),"preview":created})\n    payload={"version":1,"action":"knowledge_list","message":f"{len(items)}件のナレッジ","items":items}\n    return {"answer":json.dumps(payload,ensure_ascii=False)}''',
        [("body", ["native_list_http", "body"])], {"answer": "string"}, 6450, 1020)
    direct_list_answer = answer_node(answer_template, "native_list_answer", "非AI 一覧表示",
                                     "{{#native_list_format.answer#}}", 6800, 1020)

    build_code = '''import json\ndef main(action:str,query:str,category:str,tag:str)->dict:\n    q=(query or "").strip()\n    if action=="todo_list": q="TODO 未完了 status open"\n    elif action=="read_later_list": q="あとで読む 未読 read_later true"\n    parts=[q]\n    if (category or "").strip(): parts.append("category: "+category.strip())\n    if (tag or "").strip(): parts.append("tags: "+tag.strip())\n    return {"payload":json.dumps({"query":" ".join(x for x in parts if x).strip() or "MemoBrain"},ensure_ascii=False)}'''
    search_build = code_node(code_template, "native_search_build", "非AI 検索条件作成", build_code,
        [("action", ["start", "action"]), ("query", ["start", "query"]),
         ("category", ["start", "category"]), ("tag", ["start", "tag"])],
        {"payload": "string"}, 6100, 1240)
    search_http = node(http_post_template, "native_search_http", "非AI Knowledge検索", 6450, 1240)
    search_http["data"]["body"]["data"][0]["id"] = "key-value-native-search"
    search_http["data"]["body"]["data"][0]["value"] = "{{#native_search_build.payload#}}"

    format_code = r'''import json,re
def field(text,name):
    m=re.search(r"(?mi)^"+re.escape(name)+r":\s*(.*)$",text or "")
    return (m.group(1).strip() if m else "")
def truthy(v): return (v or "").strip().lower() in ("1","true","yes","on","はい")
def main(action:str,body:str,category:str,tag:str)->dict:
    try: data=json.loads(body or "{}")
    except: data={}
    rows=[]
    for record in data.get("records") or []:
        seg=(record or {}).get("segment") or {}; doc=seg.get("document") or {}; content=str(seg.get("content") or "")
        title=str(doc.get("name") or field(content,"title") or "(無題)")
        cat=field(content,"category"); tags=field(content,"tags"); status=field(content,"status").lower()
        todo=field(content,"todo"); read_later=field(content,"read_later")
        if (category or "").strip() and (category or "").strip().lower() not in cat.lower(): continue
        if (tag or "").strip() and (tag or "").strip().lower() not in tags.lower(): continue
        if action=="todo_list" and (status in ("done","closed","complete") or not (todo or field(content,"type").lower()=="todo")): continue
        if action=="read_later_list" and not (truthy(read_later) or "あとで読む" in content): continue
        rows.append({"title":title,"document_id":str(seg.get("document_id") or doc.get("id") or ""),"content":content})
    first=rows[0] if rows else {"title":"","document_id":"","content":""}
    if action=="knowledge_detail":
        items=[] if not rows else [{"title":first["title"],"document_id":first["document_id"],"preview":first["content"]}]
    else:
        items=[{"title":x["title"],"document_id":x["document_id"],"preview":re.sub(r"\s+"," ",x["content"])[:240]} for x in rows[:50]]
    message=("対象が見つかりませんでした。" if not items else (f"{len(items)}件見つかりました。" if action!="knowledge_detail" else "ナレッジ詳細"))
    answer=json.dumps({"version":1,"action":action,"message":message,"items":items},ensure_ascii=False)
    return {"answer":answer,"found":"true" if rows else "false","document_id":first["document_id"],"document_name":first["title"],"content":first["content"]}'''
    search_format = code_node(code_template, "native_search_format", "非AI 検索結果JSON整形", format_code,
        [("action", ["start", "action"]), ("body", ["native_search_http", "body"]),
         ("category", ["start", "category"]), ("tag", ["start", "tag"])],
        {"answer": "string", "found": "string", "document_id": "string",
         "document_name": "string", "content": "string"}, 6800, 1240)

    mutation_router = node(router_template, "native_mutation_router", "更新操作判定", 7150, 1240)
    mutation_router["data"]["cases"] = [{
        "case_id": "mutation", "id": "mutation", "logical_operator": "or",
        "conditions": [
            {"comparison_operator": "is", "id": f"mutation-{a}", "value": a,
             "varType": "string", "variable_selector": ["start", "action"]}
            for a in ["todo_complete", "read_later_complete"]
        ]
    }]
    direct_answer = answer_node(answer_template, "native_search_answer", "非AI 検索・詳細表示",
                                "{{#native_search_format.answer#}}", 7500, 1120)
    found_router = node(router_template, "native_found_router", "更新対象確認", 7500, 1370)
    found_router["data"]["cases"] = [{
        "case_id": "found", "id": "found", "logical_operator": "and",
        "conditions": [{"comparison_operator": "is", "id": "native-found", "value": "true",
                        "varType": "string", "variable_selector": ["native_search_format", "found"]}]
    }]
    not_found = answer_node(answer_template, "native_not_found", "非AI 更新対象なし",
                            '{"version":1,"action":"error","message":"更新対象が見つかりませんでした。","items":[]}', 7850, 1480)

    update_code = r'''import json,re
from datetime import datetime,timezone,timedelta
def setf(text,key,value):
    pattern=rf"(?mi)^{re.escape(key)}:\s*.*$"; line=f"{key}: {value}"
    return re.sub(pattern,line,text,count=1) if re.search(pattern,text) else line+"\n"+text
def main(action:str,content:str,name:str)->dict:
    text=content or ""; now=datetime.now(timezone.utc).astimezone(timezone(timedelta(hours=9))).strftime("%Y-%m-%d %H:%M:%S")
    if action=="todo_complete": text=setf(text,"status","done"); message=f"✅ 「{name}」を完了にしました。"
    else: text=setf(text,"read_later","false"); text=setf(text,"read_status","read"); message=f"✅ 「{name}」を読了にしました。"
    text=setf(text,"updated_at",now)
    payload={"name":name or "メモ","text":text,"doc_form":"text_model","doc_language":"Japanese"}
    answer={"version":1,"action":action,"message":message,"items":[]}
    return {"payload":json.dumps(payload,ensure_ascii=False),"answer":json.dumps(answer,ensure_ascii=False)}'''
    update_build = code_node(code_template, "native_update_build", "非AI 完了・読了データ作成", update_code,
        [("action", ["start", "action"]), ("content", ["native_search_format", "content"]),
         ("name", ["native_search_format", "document_name"])],
        {"payload": "string", "answer": "string"}, 7850, 1320)
    update_http = node(by_id["update_http"], "native_update_http", "非AI TODO完了・読了更新", 8200, 1320)
    update_http["data"]["url"] = "{{#env.DIFY_API_BASE#}}/datasets/{{#conversation.dataset_id#}}/documents/{{#native_search_format.document_id#}}/update-by-text"
    update_http["data"]["body"]["data"][0]["id"] = "key-value-native-update"
    update_http["data"]["body"]["data"][0]["value"] = "{{#native_update_build.payload#}}"
    update_answer = answer_node(answer_template, "native_update_answer", "非AI 更新完了",
                                "{{#native_update_build.answer#}}", 8550, 1320)

    nodes.extend([entry_router, native_router, direct_list_http, direct_list_format, direct_list_answer,
                  search_build, search_http, search_format, mutation_router, direct_answer,
                  found_router, not_found, update_build, update_http, update_answer])

    # Insert the early bypass and route all resolved Knowledge IDs through the native router.
    edges[:] = [item for item in edges if not (
        (item["source"] == "key_check" and item["target"] == "source_router") or
        (item["target"] == "action_router" and item["source"] in {"cache_check", "assign_existing", "assign_created"})
    )]
    edges.extend([
        edge(edge_template, "key_check", "has_key", "native_entry_router", "if-else", "if-else"),
        edge(edge_template, "native_entry_router", "native", "cache_check", "if-else", "if-else"),
        edge(edge_template, "native_entry_router", "false", "source_router", "if-else", "if-else"),
        edge(edge_template, "cache_check", "cached", "native_action_router", "if-else", "if-else"),
        edge(edge_template, "assign_existing", "source", "native_action_router", "assigner", "if-else"),
        edge(edge_template, "assign_created", "source", "native_action_router", "assigner", "if-else"),
        edge(edge_template, "native_action_router", "false", "action_router", "if-else", "if-else"),
        edge(edge_template, "native_action_router", "knowledge_list", "native_list_http", "if-else", "http-request"),
        edge(edge_template, "native_list_http", "source", "native_list_format", "http-request", "code"),
        edge(edge_template, "native_list_format", "source", "native_list_answer", "code", "answer"),
    ])
    for action in actions:
        if action == "knowledge_list":
            continue
        edges.append(edge(edge_template, "native_action_router", action, "native_search_build", "if-else", "code"))
    edges.extend([
        edge(edge_template, "native_search_build", "source", "native_search_http", "code", "http-request"),
        edge(edge_template, "native_search_http", "source", "native_search_format", "http-request", "code"),
        edge(edge_template, "native_search_format", "source", "native_mutation_router", "code", "if-else"),
        edge(edge_template, "native_mutation_router", "false", "native_search_answer", "if-else", "answer"),
        edge(edge_template, "native_mutation_router", "mutation", "native_found_router", "if-else", "if-else"),
        edge(edge_template, "native_found_router", "found", "native_update_build", "if-else", "code"),
        edge(edge_template, "native_found_router", "false", "native_not_found", "if-else", "answer"),
        edge(edge_template, "native_update_build", "source", "native_update_http", "code", "http-request"),
        edge(edge_template, "native_update_http", "source", "native_update_answer", "http-request", "answer"),
    ])

    TARGET.write_text(yaml.safe_dump(document, allow_unicode=True, sort_keys=False, width=1000), encoding="utf-8")
    print(TARGET)


if __name__ == "__main__":
    main()
