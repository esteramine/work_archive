from os import listdir
from os.path import isfile, join

# read all excel files in the current folder
onlyfiles = [f for f in listdir('./') if isfile(join('./', f)) and f.split('.')[-1] == 'xlsx']
