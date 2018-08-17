import os
import json
import urllib

def get_app_id():
    """ Use HTTP API to get and parse the application ID """
    pass 

def exec_kill_request():
    """ HTTP request to our API """
    appid = get_app_id()
    urlbase = 'http://localhost:4040/api/v1/applications/'
    apipath = '/reclaim-executor?dryRun=1'
    url = urlbase+appid+apipath 

    #urllib.request(url)
    pass


def parse_kill_status():
    """ From json, parse the output if we get any """

    # { "id" : "2",
    # "host" : "172.31.15.26",
    # "cpu" : 4,
    # "mem" : 384093388 }
    
    pass


def start_load_on(host, cpu, mem):
    """ On a given machine, start lookbusy with cpu and memory """
    #lookbusy 
